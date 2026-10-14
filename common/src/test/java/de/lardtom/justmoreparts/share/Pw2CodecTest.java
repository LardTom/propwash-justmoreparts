package de.lardtom.justmoreparts.share;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.DronePreset;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Pw2CodecTest {
    private static ShareDecoded offline(String code) {
        return ShareCodec.decode(code, ShareCodec.PartLookup.NONE);
    }

    private static Identifier id(String text) {
        return Identifier.parse(text);
    }

    static List<DroneData> presets() {
        PartFiles.registerAll();
        List<DroneData> out = new ArrayList<>();
        for (DronePreset preset : PropwashApi.builds().presets()) {
            out.add(preset.data());
        }
        for (JmpPresets.Entry entry : JmpPresets.ALL) {
            out.add(entry.data());
        }
        return out;
    }

    @Test
    public void newCodesArePw2() {
        String code = ShareCodec.encode(ShareVectors.contentB());
        assertTrue(code.startsWith("PW2"), code);
        assertTrue(code.substring(3).chars().allMatch(c -> Base62.ALPHABET.indexOf(c) >= 0), code);
        assertTrue(ShareCodec.encodePw1(ShareVectors.contentB()).startsWith("PW1-"));
    }

    @Test
    public void everyPw2VectorMatchesItsExpectation() {
        assertEquals(9, ShareVectors.pw2().size());
        for (ShareVectors.Vector vector : ShareVectors.pw2()) {
            assertEquals(Pw2Tables.FORMAT, vector.format());
            assertEquals(List.of(), ShareVectors.check(vector, offline(vector.code())), vector.name());
        }
    }

    @Test
    public void handBuiltVectorESkipsWhatItDoesNotKnow() {
        BitWriter w = new BitWriter();
        w.bit(true).bit(true).bit(false).bit(false).bit(true);
        w.golomb(200, Pw2Tables.K_PART);
        BuildSpec a = ShareVectors.buildA();
        List<Identifier> rest = List.of(a.stack(), a.motor(), a.prop(), a.video(), a.battery());
        for (int i = 0; i < rest.size(); i++) {
            w.golomb(Pw2Tables.parts(Pw2Tables.KINDS.get(i + 1)).indexOf(rest.get(i).toString()) + 1, Pw2Tables.K_PART);
        }
        w.golomb(1, 0).golomb(5, 0).golomb(1, Pw2Tables.K_PART);
        w.golomb(14, Pw2Tables.K_PAINT_SLOTS);
        for (int i = 0; i < 14; i++) {
            w.bit(i == 0 || i == 13);
        }
        w.bit(true).bits(0x112233, 24);
        w.bit(false);
        w.golomb(2, Pw2Tables.K_TUNE_COUNT);
        w.golomb(39, Pw2Tables.K_TUNE_GAP).bits(0, 1).golomb(2, Pw2Tables.K_VALUE);
        w.golomb(159, Pw2Tables.K_TUNE_GAP).bits(0, 1).golomb(14, Pw2Tables.K_VALUE);
        w.golomb(1, 0);
        w.bits(0b10, 2).golomb(11, Pw2Tables.K_TEXT);
        for (char c : "vtx.channel".toCharArray()) {
            w.bits(c - 0x20, 7);
        }
        w.bits(0, 1).golomb(6, Pw2Tables.K_VALUE);
        w.golomb(1, 0).golomb(1, 0).golomb(10, Pw2Tables.K_EXTENSION_BITS).bits(0b1011001110, 10);
        assertEquals(ShareVectors.P2_E, frame(w.toByteArray()));
        ShareDecoded decoded = offline(ShareVectors.P2_E);
        assertEquals(ShareVectors.P2_E_SKIPPED, decoded.skipped());
        assertEquals("?frame.200", ShareTables.display(decoded.require().build().frame()));
        assertEquals(Map.of("frame", 0x112233), decoded.require().paint().orElseThrow().colors());
        assertEquals(Map.of("pid.airmode", 1.0F), decoded.require().tune().orElseThrow().values());
        PartFiles.registerAll();
        ShareDecoded online = ShareCodec.decode(ShareVectors.P2_E);
        assertEquals(List.of(id("pw2.unknown:frame.200")), online.unknownParts());
    }

    static String frame(byte[] payload) {
        CRC32 crc = new CRC32();
        crc.update(payload);
        int check = (int) (crc.getValue() & 0xFFFF);
        byte[] frame = Arrays.copyOf(payload, payload.length + 2);
        frame[payload.length] = (byte) (check >>> 8);
        frame[payload.length + 1] = (byte) check;
        return "PW2" + Base62.encode(frame);
    }

    @Test
    public void unknownPartNumbersStayAndRoundTrip() {
        ShareDecoded decoded = offline(ShareVectors.P2_E);
        ShareContent content = decoded.require();
        String again = ShareCodec.encode(content);
        assertEquals(content, offline(again).require());
        Identifier marker = Identifier.fromNamespaceAndPath(Pw2Tables.UNKNOWN_NAMESPACE, "frame.3");
        assertThrows(IllegalArgumentException.class, () -> ShareCodec.encode(ShareContent.of(ShareVectors.buildA().withPart(PartKind.FRAME, marker))));
        Identifier wrongKind = Identifier.fromNamespaceAndPath(Pw2Tables.UNKNOWN_NAMESPACE, "motor.900");
        assertThrows(IllegalArgumentException.class, () -> ShareCodec.encode(ShareContent.of(ShareVectors.buildA().withPart(PartKind.FRAME, wrongKind))));
    }

    @Test
    public void publishedPw1CodesStillImport() throws Exception {
        PartFiles.registerAll();
        JsonArray vectors;
        try (InputStream in = Pw2CodecTest.class.getResourceAsStream("/share/pw1-vectors-1.0.0.json")) {
            assertNotNull(in, "pw1-vectors-1.0.0.json fehlt");
            vectors = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        }
        assertEquals(61, vectors.size());
        int presets = 0;
        for (JsonElement element : vectors) {
            JsonObject vector = element.getAsJsonObject();
            String name = vector.get("name").getAsString();
            ShareDecoded decoded = ShareCodec.decode(vector.get("code").getAsString());
            assertEquals(vector.get("status").getAsString(), decoded.status().name(), name);
            List<String> layers = new ArrayList<>();
            decoded.layers().forEach(layer -> layers.add(layer.key()));
            List<String> expectedLayers = new ArrayList<>();
            vector.getAsJsonArray("layers").forEach(layer -> expectedLayers.add(layer.getAsString()));
            assertEquals(expectedLayers, layers, name);
            if (!decoded.ok()) {
                continue;
            }
            JsonObject expected = vector.getAsJsonObject("decoded");
            ShareContent content = decoded.require();
            JsonObject build = expected.getAsJsonObject("build");
            assertEquals(build.get("frame").getAsString(), ShareTables.display(content.build().frame()), name);
            assertEquals(build.get("stack").getAsString(), ShareTables.display(content.build().stack()), name);
            assertEquals(build.get("motor").getAsString(), ShareTables.display(content.build().motor()), name);
            assertEquals(build.get("prop").getAsString(), ShareTables.display(content.build().prop()), name);
            assertEquals(build.get("video").getAsString(), ShareTables.display(content.build().video()), name);
            assertEquals(build.get("battery").getAsString(), ShareTables.display(content.build().battery()), name);
            if (!expected.get("paint").isJsonNull()) {
                Map<String, Integer> paint = new TreeMap<>();
                expected.getAsJsonObject("paint").entrySet().forEach(e -> paint.put(e.getKey(), Integer.parseInt(e.getValue().getAsString().substring(1), 16)));
                assertEquals(paint, new TreeMap<>(content.paint().orElseThrow().colors()), name);
            }
            if (!expected.get("tune").isJsonNull()) {
                Map<String, Float> tune = new TreeMap<>();
                expected.getAsJsonObject("tune").entrySet().forEach(e -> tune.put(e.getKey(), e.getValue().getAsFloat()));
                assertEquals(tune, new TreeMap<>(content.tune().orElseThrow().values()), name);
            }
            if (name.startsWith("preset")) {
                presets++;
                assertTrue(decoded.partsKnown(), name + " " + decoded.unknownParts());
                ShareDecoded pw2 = ShareCodec.decode(ShareCodec.encode(content));
                assertEquals(content, pw2.require(), name);
                assertTrue(ShareCodec.encode(content).length() * 5 < vector.get("code").getAsString().length(), name);
            }
        }
        assertEquals(54, presets);
    }

    @Test
    public void everyPresetRoundTripsWithEveryLayerMix() {
        List<DroneData> presets = presets();
        assertEquals(27, presets.size());
        ShareOsd osd = new ShareOsd(1, ShareVectors.osdP2C());
        TuneSpec tune = new TuneSpec(Map.of("rates.roll.rcRate", 1.25F, "rates.pitch.rcRate", 1.25F, "pid.roll.p", 48.0F,
                "pid.motorIdlePercent", 5.5F, TuneSpec.CAMERA_UPTILT, 35.0F));
        for (DroneData preset : presets) {
            DroneData data = new DroneData(preset.build(), preset.paint(), tune, Optional.of("Night Ripper"));
            for (int mask = 0; mask < 16; mask++) {
                EnumSet<ShareLayer> layers = EnumSet.of(ShareLayer.PARTS);
                if ((mask & 1) != 0) {
                    layers.add(ShareLayer.PAINT);
                }
                if ((mask & 2) != 0) {
                    layers.add(ShareLayer.TUNE);
                }
                if ((mask & 4) != 0) {
                    layers.add(ShareLayer.OSD);
                }
                if ((mask & 8) != 0) {
                    layers.add(ShareLayer.NAME);
                }
                ShareContent content = ShareContent.of(data, layers, Optional.of(osd));
                String code = ShareCodec.encode(content);
                ShareDecoded decoded = ShareCodec.decode(code);
                assertEquals(ShareStatus.OK, decoded.status(), code);
                assertEquals(content, decoded.require(), code);
                assertEquals(List.copyOf(content.layers()), decoded.layers(), code);
                assertTrue(decoded.partsKnown(), code);
                assertTrue(ShareCodec.fits(code));
                String pw1 = ShareCodec.encodePw1(content);
                assertTrue(code.length() * 2 < pw1.length(), code + " gegen " + pw1);
            }
            assertTrue(ShareCodec.encode(ShareContent.of(preset.build())).length() <= 20);
        }
    }

    @Test
    public void tablesAreAppendOnlyAndCoverEveryPart() {
        PartFiles.registerAll();
        for (PartKind kind : Pw2Tables.KINDS) {
            List<String> table = Pw2Tables.parts(kind);
            assertEquals(table.size(), new HashSet<>(table).size(), kind.name());
            for (PartDefinition part : PropwashApi.parts().all(kind)) {
                if (part.id().getNamespace().equals("propwash") || part.id().getNamespace().equals("justmoreparts")) {
                    assertTrue(table.contains(part.id().toString()), "PW2-Tabelle ohne " + part.id() + " (anhängen, nie einfügen)");
                }
            }
        }
        assertEquals(27, Pw2Tables.FRAMES.size());
        assertEquals(16, Pw2Tables.STACKS.size());
        assertEquals(40, Pw2Tables.MOTORS.size());
        assertEquals(44, Pw2Tables.PROPS.size());
        assertEquals("justmoreparts:p89_30_3b_pc", Pw2Tables.PROPS.get(42));
        assertEquals("propwash:p51_29_3b", Pw2Tables.PROPS.get(43));
        assertEquals(11, Pw2Tables.VIDEOS.size());
        assertEquals(35, Pw2Tables.BATTERIES.size());
        assertEquals(5, Pw2Tables.ACCESSORIES.size());
        assertEquals("propwash:cinewhoop_3", Pw2Tables.FRAMES.get(0));
        assertEquals("justmoreparts:xclass_13_h", Pw2Tables.FRAMES.get(26));
        assertEquals(51, Pw2Tables.COLORS.size());
        assertEquals(Pw2Tables.COLORS.size(), new HashSet<>(Pw2Tables.COLORS).size());
        assertEquals(ShareTables.TUNE_KEYS, Pw2Tables.TUNE_KEYS.subList(0, ShareTables.TUNE_KEYS.size()));
        assertEquals("pid.armMaxAngleDeg", Pw2Tables.TUNE_KEYS.get(56));
        assertEquals(8429, Pw2Tables.OSD_DICTIONARY.length);
        assertEquals(64, Pw2Tables.TEXT_ALPHABET.length());
        assertEquals(62, Base62.ALPHABET.length());
        CRC32 crc = new CRC32();
        crc.update(Pw2Tables.OSD_DICTIONARY);
        assertEquals(0xb87eac81L, crc.getValue(), "OSD-Wörterbuch ist eingefroren");
    }

    @Test
    public void base62KeepsLengthAndLeadingZeros() throws ShareFormatException {
        int previous = -1;
        for (int bytes = 0; bytes <= 2600; bytes++) {
            int symbols = Base62.symbolsFor(bytes);
            assertTrue(symbols > previous, "streng steigend bei " + bytes);
            assertEquals(bytes, Base62.bytesFor(symbols));
            previous = symbols;
        }
        Random random = new Random(3);
        for (int round = 0; round < 500; round++) {
            byte[] data = new byte[random.nextInt(120)];
            random.nextBytes(data);
            if (data.length > 0 && random.nextBoolean()) {
                data[0] = 0;
            }
            String text = Base62.encode(data);
            assertEquals(Base62.symbolsFor(data.length), text.length());
            assertArrayEquals(data, Base62.decode(text));
        }
        assertEquals("", Base62.encode(new byte[0]));
        assertEquals("00", Base62.encode(new byte[]{0}));
        assertEquals("47", Base62.encode(new byte[]{(byte) 0xFF}));
        assertThrows(ShareFormatException.class, () -> Base62.decode("48"));
        assertThrows(ShareFormatException.class, () -> Base62.decode("0_"));
    }

    @Test
    public void statusValuesForBrokenPw2Input() {
        String a = ShareVectors.P2_A;
        assertEquals(ShareStatus.OK, offline(a).status());
        assertEquals(ShareStatus.OK, offline("  pw2" + a.substring(3, 7) + "-\n" + a.substring(7) + " ").status());
        assertEquals(ShareStatus.OK, offline("PW2-" + a.substring(3)).status());
        assertEquals(ShareStatus.CHECKSUM, offline(ShareVectors.P2_F).status());
        assertEquals(ShareStatus.BAD_CHARACTER, offline("PW2" + a.substring(3, 8) + "_" + a.substring(9)).status());
        assertEquals(ShareStatus.TRUNCATED, offline("PW2").status());
        assertEquals(ShareStatus.TRUNCATED, offline("PW2" + "0".repeat(4)).status());
        assertEquals(ShareStatus.UNSUPPORTED_VERSION, offline("PW3" + a.substring(3)).status());
        assertEquals(ShareStatus.UNSUPPORTED_VERSION, offline("pw9-abc").status());
        assertEquals(ShareStatus.INVALID_PREFIX, offline("PWX" + a.substring(3)).status());
        assertEquals(ShareStatus.TOO_LONG, offline("PW2" + "0".repeat(ShareTables.MAX_INPUT_CHARS)).status());
        assertEquals(ShareStatus.TOO_LARGE, offline("PW2" + "0".repeat(Base62.symbolsFor(ShareTables.MAX_FRAME_BYTES + 1))).status());
        assertNotEquals(ShareStatus.OK, offline(a.toLowerCase()).status());
        assertEquals(ShareStatus.CORRUPT, offline("PW2" + "z".repeat(Base62.symbolsFor(5))).status());
        BitWriter padded = new BitWriter();
        writeA(padded);
        byte[] bytes = padded.toByteArray();
        bytes[bytes.length - 1] |= 1;
        assertEquals(ShareStatus.CORRUPT, offline(frame(bytes)).status());
        BitWriter extra = new BitWriter();
        writeA(extra);
        extra.bits(0, 8);
        assertEquals(ShareStatus.CORRUPT, offline(frame(extra.toByteArray())).status());
        BitWriter cut = new BitWriter();
        cut.bits(0, 5).golomb(2, Pw2Tables.K_PART);
        assertEquals(ShareStatus.TRUNCATED, offline(frame(cut.toByteArray())).status());
    }

    private static void writeA(BitWriter w) {
        w.bits(0, 5);
        BuildSpec a = ShareVectors.buildA();
        List<Identifier> six = List.of(a.frame(), a.stack(), a.motor(), a.prop(), a.video(), a.battery());
        for (int i = 0; i < six.size(); i++) {
            w.golomb(Pw2Tables.parts(Pw2Tables.KINDS.get(i)).indexOf(six.get(i).toString()) + 1, Pw2Tables.K_PART);
        }
        w.golomb(0, 0);
    }

    @Test
    public void handBuiltPw2ReproducesVectorA() {
        BitWriter w = new BitWriter();
        writeA(w);
        assertEquals(ShareVectors.P2_A, frame(w.toByteArray()));
    }

    @Test
    public void valuesTravelInTheShortestExactForm() {
        Map<String, Float> values = new LinkedHashMap<>();
        values.put("rates.roll.rcRate", 1.25F);
        values.put("rates.pitch.rcRate", 1.25F);
        values.put("pid.roll.p", 48.0F);
        values.put("pid.motorIdlePercent", 5.5F);
        values.put("rates.rcDeadband", 0.015F);
        values.put("pid.throttleMid", 1.0F / 3.0F);
        values.put("pid.yaw.p", -0.0F);
        values.put("pid.roll.ff", -12.0F);
        values.put("filters.gyroLpfHz", 1.0E9F);
        values.put("pid.horizonStrength", Float.MIN_VALUE);
        values.put("pid.angleStrength", -Float.MAX_VALUE);
        TuneSpec tune = new TuneSpec(values);
        ShareContent content = new ShareContent(ShareVectors.buildA(), Optional.empty(), Optional.of(tune), Optional.empty(), Optional.empty());
        TuneSpec back = offline(ShareCodec.encode(content)).require().tune().orElseThrow();
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            assertEquals(Float.floatToRawIntBits(entry.getValue()), Float.floatToRawIntBits(back.values().get(entry.getKey())), entry.getKey());
        }
        assertEquals(0L, Pw2Codec.scaled(0.0F, 0));
        assertEquals(Long.MIN_VALUE, Pw2Codec.scaled(-0.0F, 0));
        assertEquals(55L, Pw2Codec.scaled(5.5F, 1));
        assertEquals(Long.MIN_VALUE, Pw2Codec.scaled(5.5F, 0));
        assertEquals(15L, Pw2Codec.scaled(0.015F, 3));
        assertEquals(Long.MIN_VALUE, Pw2Codec.scaled(1.0F / 3.0F, 3));
    }

    @Test
    public void osdTravelsDeflatedAgainstThePresetsOrAsText() {
        ShareContent base = ShareContent.of(ShareVectors.buildA());
        ShareContent full = new ShareContent(base.build(), Optional.empty(), Optional.empty(), Optional.of(new ShareOsd(1, ShareVectors.osdP2C())),
                Optional.empty());
        String code = ShareCodec.encode(full);
        assertTrue(code.length() < 80, code);
        assertEquals(full, offline(code).require());
        String json = "{\"v\":1,\"x\":\"" + "q".repeat(8000) + "\"}";
        ShareContent big = new ShareContent(base.build(), Optional.empty(), Optional.empty(), Optional.of(new ShareOsd(7, json)), Optional.empty());
        assertEquals(big, offline(ShareCodec.encode(big)).require());
        assertEquals(ShareVectors.contentP2I(), offline(ShareVectors.P2_I).require());
    }

    @Test
    public void namesAndForeignIdsUseTheirTextForms() {
        for (String name : List.of("Night Ripper", "X-Class 13", "Race #1 (v2)", "Flitzer Ä 🚀 ß", "a", "x".repeat(48))) {
            ShareContent content = new ShareContent(ShareVectors.buildA(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(name));
            assertEquals(content, offline(ShareCodec.encode(content)).require(), name);
        }
        BuildSpec foreign = ShareVectors.buildA().withPart(PartKind.FRAME, id("otheraddon:frame_x"))
                .withPart(PartKind.MOTOR, id("propwash:m_newer_motor")).withAccessory(AccessorySlot.TOP, id("a.b-c_d:x.y-z_0"));
        ShareDecoded decoded = offline(ShareCodec.encode(ShareContent.of(foreign)));
        assertEquals(foreign, decoded.require().build());
        Identifier pw1Unknown = ShareTables.unknownNamespace(7, "mystery_frame");
        BuildSpec carried = ShareVectors.buildA().withPart(PartKind.FRAME, pw1Unknown);
        ShareDecoded fromPw1 = offline(ShareCodec.encode(ShareContent.of(carried)));
        assertEquals(carried, fromPw1.require().build());
        assertEquals("?7:mystery_frame", ShareTables.display(fromPw1.require().build().frame()));
        assertThrows(IllegalArgumentException.class, () -> ShareCodec.encode(ShareContent.of(ShareVectors.buildA().withPart(PartKind.FRAME, id("propwash:frame/x")))));
    }

    @Test
    public void randomContentRoundTrips() {
        Random random = new Random(11);
        List<String> paths = List.of("freestyle_5", "whoop_65", "deadcat_5", "m2207_1750", "a", "x.y-z_0", "p".repeat(128));
        List<String> namespaces = List.of("propwash", "justmoreparts", "otheraddon", "a.b-c_d");
        for (int round = 0; round < 1500; round++) {
            Identifier[] six = new Identifier[6];
            for (int i = 0; i < 6; i++) {
                List<String> table = Pw2Tables.parts(Pw2Tables.KINDS.get(i));
                six[i] = random.nextInt(3) > 0 ? Identifier.parse(table.get(random.nextInt(table.size())))
                        : Identifier.fromNamespaceAndPath(namespaces.get(random.nextInt(namespaces.size())), paths.get(random.nextInt(paths.size())));
            }
            EnumMap<AccessorySlot, Identifier> accessories = new EnumMap<>(AccessorySlot.class);
            for (AccessorySlot slot : AccessorySlot.values()) {
                if (random.nextBoolean()) {
                    accessories.put(slot, random.nextBoolean() ? Identifier.parse(Pw2Tables.ACCESSORIES.get(random.nextInt(5)))
                            : Identifier.fromNamespaceAndPath(namespaces.get(random.nextInt(namespaces.size())), "acc_" + random.nextInt(9)));
                }
            }
            BuildSpec build = new BuildSpec(six[0], six[1], six[2], six[3], six[4], six[5], accessories);
            PaintSpec paint = PaintSpec.DEFAULT;
            for (String slot : PaintSpec.SLOTS) {
                if (random.nextInt(3) == 0) {
                    int rgb = switch (random.nextInt(3)) {
                        case 0 -> Pw2Tables.COLORS.get(random.nextInt(Pw2Tables.COLORS.size()));
                        case 1 -> 0x1B1D20;
                        default -> random.nextInt(0x1000000);
                    };
                    paint = paint.with(slot, rgb);
                }
            }
            Map<String, Float> tune = new TreeMap<>();
            int entries = random.nextInt(60);
            for (int i = 0; i < entries; i++) {
                String key = random.nextInt(4) == 0 ? "pid.custom" + random.nextInt(100) : Pw2Tables.TUNE_KEYS.get(random.nextInt(Pw2Tables.TUNE_KEYS.size()));
                float value = switch (random.nextInt(6)) {
                    case 0 -> (float) (random.nextInt(4001) * 5 / 1000.0);
                    case 1 -> random.nextFloat() * 1000.0F - 500.0F;
                    case 2 -> (float) (random.nextInt(2001) / 10.0 - 100.0);
                    case 3 -> (float) (random.nextInt(201) / 100.0);
                    case 4 -> Float.intBitsToFloat(random.nextInt()) * 0.0F + (float) random.nextGaussian();
                    default -> (float) random.nextInt(2000);
                };
                if (Float.isFinite(value)) {
                    tune.put(key, value);
                }
            }
            Optional<TuneSpec> tuneSpec = random.nextBoolean() ? Optional.of(new TuneSpec(tune)) : Optional.empty();
            Optional<ShareOsd> osd = switch (random.nextInt(4)) {
                case 0 -> Optional.of(new ShareOsd(1 + random.nextInt(3), "{\"v\":1,\"x\":\"ä€😀" + round + "\"}"));
                case 1 -> Optional.of(new ShareOsd(1, Pw2Tables.osdPreset(List.of("full", "standard", "race", "minimal").get(random.nextInt(4)))
                        .replace("\"row\":" + random.nextInt(10), "\"row\":" + random.nextInt(15))));
                default -> Optional.empty();
            };
            Optional<String> name = switch (random.nextInt(4)) {
                case 0 -> Optional.of("Drohne Nr. " + round + " – ßü");
                case 1 -> Optional.of("Quad " + round);
                case 2 -> Optional.of("Quad #" + round + "!");
                default -> Optional.empty();
            };
            ShareContent content = new ShareContent(build, random.nextBoolean() ? Optional.of(paint) : Optional.empty(), tuneSpec, osd, name);
            String code = ShareCodec.encode(content);
            ShareDecoded decoded = offline(code);
            assertEquals(ShareStatus.OK, decoded.status(), code);
            assertEquals(content, decoded.require(), code);
            assertEquals(List.copyOf(content.layers()), decoded.layers());
            assertTrue(code.startsWith(Pw2Tables.PREFIX));
            assertEquals(code, ShareCodec.encode(decoded.require()));
        }
    }

    @Test
    public void randomDamageNeverThrows() {
        Random random = new Random(5);
        List<byte[]> seeds = new ArrayList<>();
        for (ShareVectors.Vector vector : ShareVectors.pw2()) {
            if (vector.status() == ShareStatus.OK) {
                seeds.add(rawFrame(vector.code()));
            }
        }
        int ok = 0;
        Set<ShareStatus> seen = EnumSet.noneOf(ShareStatus.class);
        for (int round = 0; round < 20000; round++) {
            byte[] frame = seeds.get(random.nextInt(seeds.size())).clone();
            int body = frame.length - 2;
            int flips = 1 + random.nextInt(4);
            for (int i = 0; i < flips; i++) {
                int at = random.nextInt(body);
                frame[at] = (byte) (random.nextInt(4) == 0 ? random.nextInt(256) : frame[at] ^ (1 << random.nextInt(8)));
            }
            byte[] payload = Arrays.copyOf(frame, random.nextInt(5) == 0 ? 1 + random.nextInt(body) : body);
            ShareDecoded decoded = offline(frame(payload));
            assertNotNull(decoded);
            assertNotEquals(ShareStatus.CHECKSUM, decoded.status());
            seen.add(decoded.status());
            if (decoded.ok()) {
                ok++;
                assertNotNull(ShareCodec.encode(decoded.require()));
            }
        }
        assertTrue(ok > 0);
        assertTrue(seen.contains(ShareStatus.CORRUPT) && seen.contains(ShareStatus.TRUNCATED), seen.toString());
        for (int round = 0; round < 5000; round++) {
            StringBuilder text = new StringBuilder("PW2");
            int length = random.nextInt(300);
            for (int i = 0; i < length; i++) {
                text.append(Base62.ALPHABET.charAt(random.nextInt(62)));
            }
            assertNotNull(offline(text.toString()));
        }
    }

    private static byte[] rawFrame(String code) {
        try {
            return Base62.decode(code.substring(3));
        } catch (ShareFormatException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void codesAreMuchShorterThanPw1() {
        for (ShareVectors.Vector vector : ShareVectors.pw1()) {
            if (vector.content().isEmpty() || !vector.skipped().isEmpty()) {
                continue;
            }
            String pw2 = ShareCodec.encode(vector.content().get());
            assertTrue(pw2.length() * 2 < vector.code().length(), vector.name() + ": " + pw2.length() + " gegen " + vector.code().length());
        }
        assertEquals(13, ShareVectors.P2_A.length());
        assertFalse(ShareVectors.P2_A.contains("-"));
    }
}
