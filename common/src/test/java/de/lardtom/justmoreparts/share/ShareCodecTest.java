package de.lardtom.justmoreparts.share;

import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneGroup;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShareCodecTest {
    public static final String A = ShareVectors.A;
    public static final String B = ShareVectors.B;
    public static final String C = ShareVectors.C;
    public static final String D = ShareVectors.D;
    public static final String E = ShareVectors.E;
    public static final String F = ShareVectors.F;

    private static Identifier id(String text) {
        return Identifier.parse(text);
    }

    private static BuildSpec buildA() {
        return ShareVectors.buildA();
    }

    private static ShareContent contentA() {
        return ShareVectors.contentA();
    }

    private static ShareContent contentB() {
        return ShareVectors.contentB();
    }

    private static ShareContent contentC() {
        return ShareVectors.contentC();
    }

    private static ShareContent contentD() {
        return ShareVectors.contentD();
    }

    private static ShareDecoded decodeOffline(String code) {
        return ShareCodec.decode(code, ShareCodec.PartLookup.NONE);
    }

    @Test
    public void tablesMatchPropwash() {
        assertEquals(PaintSpec.SLOTS, ShareTables.PAINT_SLOTS);
        assertEquals(56, ShareTables.TUNE_KEYS.size());
        assertEquals(ShareTables.TUNE_KEYS.size(), ShareTables.TUNE_KEYS.stream().distinct().count());
        for (String key : ShareTables.TUNE_KEYS.subList(0, 55)) {
            assertTrue(TuneGroup.FLIGHT.contains(TuneGroup.of(key)), key);
        }
        assertEquals(1, ShareTables.tuneIndex("rates.type"));
        assertEquals(40, ShareTables.tuneIndex("pid.airmode"));
        assertEquals(55, ShareTables.tuneIndex("filters.rcSmoothingHz"));
        assertEquals(56, ShareTables.tuneIndex(TuneSpec.CAMERA_UPTILT));
        assertEquals(TuneGroup.CAMERA, TuneGroup.of(TuneSpec.CAMERA_UPTILT));
        assertTrue(ShareTables.TUNE_GROUPS.containsAll(TuneGroup.FLIGHT));
        assertEquals(TuneGroup.FLIGHT.size() + 1, ShareTables.TUNE_GROUPS.size());
        assertEquals(32, ShareTables.ALPHABET.length());
    }

    @Test
    public void vectorsEncodeBytewiseWithoutCompression() {
        assertEquals(A, ShareCodec.encodePw1(contentA(), ShareCompression.NEVER));
        assertEquals(B, ShareCodec.encodePw1(contentB(), ShareCompression.NEVER));
        assertEquals(D, ShareCodec.encodePw1(contentD(), ShareCompression.NEVER));
    }

    @Test
    public void vectorADecodes() {
        ShareDecoded decoded = decodeOffline(A);
        assertEquals(ShareStatus.OK, decoded.status());
        assertEquals(List.of(ShareLayer.PARTS), decoded.layers());
        assertEquals(contentA(), decoded.require());
        assertEquals(List.of(), decoded.skipped());
    }

    @Test
    public void vectorBDecodes() {
        ShareDecoded decoded = decodeOffline(B);
        assertEquals(ShareStatus.OK, decoded.status());
        assertEquals(List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.NAME), decoded.layers());
        assertEquals(contentB(), decoded.require());
        assertEquals(0.4567891F, decoded.require().tune().orElseThrow().values().get("pid.throttleMid"));
        assertEquals(List.of(), decoded.skipped());
    }

    @Test
    public void vectorCDecodesAndCompressedRoundTrips() {
        ShareDecoded decoded = decodeOffline(C);
        assertEquals(ShareStatus.OK, decoded.status());
        assertEquals(List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.OSD, ShareLayer.NAME), decoded.layers());
        assertEquals(contentC(), decoded.require());
        String again = ShareCodec.encodePw1(contentC(), ShareCompression.ALWAYS);
        assertEquals(contentC(), decodeOffline(again).require());
        assertEquals(contentC(), decodeOffline(ShareCodec.encodePw1(contentC(), ShareCompression.AUTO)).require());
    }

    @Test
    public void vectorDKeepsForeignNamespaceAndLiteralKey() {
        ShareDecoded decoded = ShareCodec.decode(D, ShareCodec.PartLookup.PROPWASH);
        assertEquals(ShareStatus.OK, decoded.status());
        assertEquals(contentD(), decoded.require());
        assertEquals(List.of(id("otheraddon:frame_x")), decoded.unknownParts());
        assertEquals(List.of(), decoded.wrongKind());
        assertEquals(3.0F, decoded.require().tune().orElseThrow().values().get("pid.newFlightKey"));
    }

    @Test
    public void vectorESkipsWhatItDoesNotKnow() {
        ShareDecoded decoded = decodeOffline(E);
        assertEquals(ShareStatus.OK, decoded.status());
        assertEquals(List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE), decoded.layers());
        assertEquals(buildA(), decoded.require().build());
        assertEquals(Map.of("frame", 0x112233), decoded.require().paint().orElseThrow().colors());
        assertEquals(Map.of("pid.airmode", 1.0F), decoded.require().tune().orElseThrow().values());
        assertEquals(List.of("accessory_slot:5", "paint_slot:13", "tune_index:200", "tune_key:vtx.channel", "layer:6"), decoded.skipped());
    }

    @Test
    public void everyVectorMatchesItsExpectation() {
        for (ShareVectors.Vector vector : ShareVectors.all()) {
            assertEquals(List.of(), ShareVectors.check(vector, decodeOffline(vector.code())), vector.name());
        }
    }

    @Test
    public void vectorGCarriesTheCameraAngleAndOldCodesMeanTheFrameDefault() {
        ShareDecoded decoded = decodeOffline(ShareVectors.G);
        assertEquals(ShareStatus.OK, decoded.status());
        assertEquals(ShareVectors.contentG(), decoded.require());
        assertEquals(52.0F, decoded.require().tune().orElseThrow().values().get(TuneSpec.CAMERA_UPTILT));
        assertEquals(ShareVectors.G, ShareCodec.encodePw1(ShareVectors.contentG(), ShareCompression.NEVER));
        assertFalse(decodeOffline(B).require().tune().orElseThrow().values().containsKey(TuneSpec.CAMERA_UPTILT));
        TuneSpec mixed = new TuneSpec(Map.of("rates.yaw.rcRate", 1.0F, TuneSpec.CAMERA_UPTILT, 30.0F, "vtx.channel", 3.0F, "led.mode", 1.0F));
        ShareContent content = new ShareContent(buildA(), Optional.empty(), Optional.of(mixed), Optional.empty(), Optional.empty());
        assertEquals(Map.of("rates.yaw.rcRate", 1.0F, TuneSpec.CAMERA_UPTILT, 30.0F), content.tune().orElseThrow().values());
        TuneSpec stored = new TuneSpec(Map.of(TuneSpec.CAMERA_UPTILT, 45.0F, "led.mode", 2.0F));
        TuneSpec fromOldCode = stored.overlay(decodeOffline(B).require().tune().orElseThrow(), ShareTables.TUNE_GROUPS);
        assertFalse(fromOldCode.values().containsKey(TuneSpec.CAMERA_UPTILT), "alter Code ohne Winkel: Standard des Rahmens");
        assertEquals(2.0F, fromOldCode.values().get("led.mode"));
    }

    @Test
    public void vectorFFailsTheChecksum() {
        assertEquals(ShareStatus.CHECKSUM, decodeOffline(F).status());
    }

    @Test
    public void knownPartsAreClassifiedThroughPropwash() {
        PartFiles.registerAll();
        ShareDecoded a = ShareCodec.decode(A);
        assertTrue(a.partsKnown(), a.toString());
        ShareDecoded b = ShareCodec.decode(B);
        assertEquals(List.of(), b.unknownParts());
        assertEquals(List.of(), b.wrongKind());
        ShareDecoded c = ShareCodec.decode(C);
        assertEquals(List.of(), c.unknownParts());
        BuildSpec mixed = buildA().withPart(PartKind.FRAME, id("propwash:m2207_1750")).withPart(PartKind.PROP, id("justmoreparts:nope"));
        ShareDecoded wrong = ShareCodec.decode(ShareCodec.encodePw1(ShareContent.of(mixed)));
        assertEquals(List.of(id("propwash:m2207_1750")), wrong.wrongKind());
        assertEquals(List.of(id("justmoreparts:nope")), wrong.unknownParts());
        assertFalse(wrong.partsKnown());
    }

    @Test
    public void inputIsReadTolerantly() {
        ShareDecoded reference = decodeOffline(A);
        String relaxed = "pw1" + A.substring(3).toLowerCase().replace('0', 'o').replace('1', 'l');
        assertEquals(reference, decodeOffline(relaxed));
        assertEquals(reference, decodeOffline("  " + A.replace("-", " - ") + "\n"));
        assertEquals(reference, decodeOffline(A.replace("-", "")));
        assertEquals(reference, decodeOffline("PW1" + A.substring(4)));
        assertEquals(reference, decodeOffline("PW1" + A.substring(3).replace("-", "\n").replace("1", "I")));
    }

    @Test
    public void statusValuesForBrokenInput() {
        assertEquals(ShareStatus.EMPTY, decodeOffline(null).status());
        assertEquals(ShareStatus.EMPTY, decodeOffline(" \n\t").status());
        assertEquals(ShareStatus.INVALID_PREFIX, decodeOffline("JMP1:abc").status());
        assertEquals(ShareStatus.UNSUPPORTED_VERSION, decodeOffline("PW3-04002").status());
        assertEquals(ShareStatus.BAD_CHARACTER, decodeOffline("PW1-04U02").status());
        assertEquals(ShareStatus.BAD_CHARACTER, decodeOffline("PW1-04*02").status());
        assertEquals(ShareStatus.TOO_LONG, decodeOffline("PW1-" + "0".repeat(ShareTables.MAX_INPUT_CHARS)).status());
        assertEquals(ShareStatus.TOO_LONG, decodeOffline("x".repeat(100000)).status());
        assertEquals(ShareStatus.TRUNCATED, decodeOffline("PW1-04002").status());
        assertEquals(ShareStatus.TRUNCATED, decodeOffline("PW1").status());
        assertEquals(ShareStatus.CORRUPT, decodeOffline(withPaddingBit()).status());
    }

    private static String withPaddingBit() {
        for (int length = 1; length < 10; length++) {
            byte[] name = new ByteWriter().str("n".repeat(length)).toByteArray();
            String code = frame(1, 0, payload(1 | 16, partsBlock(buildA()), name));
            String symbols = code.substring(ShareTables.PREFIX.length()).replace("-", "");
            if (symbols.length() * 5 % 8 != 0) {
                char last = symbols.charAt(symbols.length() - 1);
                char changed = ShareTables.ALPHABET.charAt(ShareTables.ALPHABET.indexOf(last) ^ 1);
                return ShareTables.PREFIX + symbols.substring(0, symbols.length() - 1) + changed;
            }
        }
        throw new IllegalStateException("kein Rahmen mit Füllbits gefunden");
    }

    @Test
    public void frameRulesAreEnforced() {
        byte[] payload = ShareCodec.payload(contentA());
        assertEquals(ShareStatus.OK, decodeOffline(frame(1, 0, payload)).status());
        assertEquals(ShareStatus.UNSUPPORTED_VERSION, decodeOffline(frame(2, 0, payload)).status());
        assertEquals(ShareStatus.UNSUPPORTED_VERSION, decodeOffline(frame(1, 2, payload)).status());
        byte[] trailing = Arrays.copyOf(payload, payload.length + 1);
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, trailing)).status());
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 1, payload)).status());
        byte[] bomb = new ByteWriter().varint(1).varint(ShareTables.MAX_PAYLOAD_BYTES).bytes(new byte[ShareTables.MAX_PAYLOAD_BYTES]).toByteArray();
        assertEquals(ShareStatus.TOO_LARGE, decodeOffline(frame(1, 1, deflate(bomb))).status());
        byte[] packed = deflate(payload);
        assertEquals(ShareStatus.OK, decodeOffline(frame(1, 1, packed)).status());
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 1, Arrays.copyOf(packed, packed.length - 2))).status());
        byte[] packedTrailing = Arrays.copyOf(packed, packed.length + 1);
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 1, packedTrailing)).status());
    }

    @Test
    public void payloadRulesAreEnforced() {
        byte[] parts = partsBlock(buildA());
        assertEquals(ShareStatus.OK, decodeOffline(frame(1, 0, payload(1, parts))).status());
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, payload(2, new ByteWriter().u16(0).toByteArray()))).status());
        byte[] cut = Arrays.copyOf(parts, parts.length - 1);
        assertEquals(ShareStatus.TRUNCATED, decodeOffline(frame(1, 0, payload(1, cut))).status());
        byte[] tooLong = Arrays.copyOf(parts, parts.length + 1);
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, payload(1, tooLong))).status());
        byte[] declaredTooLong = new ByteWriter().varint(1).varint(parts.length + 5).bytes(parts).toByteArray();
        assertEquals(ShareStatus.TRUNCATED, decodeOffline(frame(1, 0, declaredTooLong)).status());
        byte[] longVarint = new ByteWriter().u8(0x81).u8(0x80).u8(0x80).u8(0x80).u8(0x80).u8(0x00).toByteArray();
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, longVarint)).status());
        byte[] name49 = new ByteWriter().str("x".repeat(49)).toByteArray();
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, payload(1 | 16, parts, name49))).status());
        byte[] name48 = new ByteWriter().str("  " + "x".repeat(48) + " ").toByteArray();
        assertEquals(Optional.of("x".repeat(48)), decodeOffline(frame(1, 0, payload(1 | 16, parts, name48))).require().name());
        byte[] blankName = new ByteWriter().str("   ").toByteArray();
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, payload(1 | 16, parts, blankName))).status());
        byte[] badUtf8 = new ByteWriter().varint(2).u8(0xC3).u8(0x28).toByteArray();
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, payload(1 | 16, parts, badUtf8))).status());
        ByteWriter manyTune = new ByteWriter().varint(ShareTables.MAX_TUNE_ENTRIES + 1);
        for (int i = 0; i <= ShareTables.MAX_TUNE_ENTRIES; i++) {
            manyTune.varint(2).zigzag(i);
        }
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, payload(1 | 4, parts, manyTune.toByteArray()))).status());
        byte[] nanTune = new ByteWriter().varint(1).varint(3).f32(Float.NaN).toByteArray();
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, payload(1 | 4, parts, nanTune))).status());
        byte[] emptyTune = new ByteWriter().varint(0).toByteArray();
        ShareDecoded reset = decodeOffline(frame(1, 0, payload(1 | 4, parts, emptyTune)));
        assertEquals(Optional.of(TuneSpec.EMPTY), reset.require().tune());
        byte[] badOsd = new ByteWriter().varint(0).str("{}").toByteArray();
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, payload(1 | 8, parts, badOsd))).status());
        byte[] bigOsd = new ByteWriter().varint(1).str("x".repeat(ShareTables.MAX_OSD_CHARS + 1)).toByteArray();
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 1, deflate(payload(1 | 8, parts, bigOsd)))).status());
        ByteWriter badPath = new ByteWriter().varint(1).str("Frame X");
        for (int i = 0; i < 5; i++) {
            badPath.varint(1).str("stack_30");
        }
        badPath.u8(0);
        assertEquals(ShareStatus.CORRUPT, decodeOffline(frame(1, 0, payload(1, badPath.toByteArray()))).status());
    }

    @Test
    public void unknownNamespaceStaysReadableAndRoundTrips() {
        ByteWriter parts = new ByteWriter().varint(7).str("frame_x");
        for (String path : List.of("stack_30", "m2207_1750", "p51_3b", "analog_race", "b6s_1300")) {
            parts.varint(1).str(path);
        }
        parts.u8(1).u8(1).varint(9).str("strip");
        String code = frame(1, 0, payload(1, parts.toByteArray()));
        ShareDecoded decoded = decodeOffline(code);
        assertEquals(ShareStatus.OK, decoded.status());
        assertEquals(List.of("namespace:7", "namespace:9"), decoded.skipped());
        assertEquals("?7:frame_x", ShareTables.display(decoded.require().build().frame()));
        assertEquals("?9:strip", ShareTables.display(decoded.require().build().accessory(AccessorySlot.BOTTOM).orElseThrow()));
        assertEquals(2, decoded.unknownParts().stream().filter(i -> ShareTables.unknownNamespaceIndex(i).isPresent()).count());
        assertEquals(code, ShareCodec.encodePw1(decoded.require(), ShareCompression.NEVER));
    }

    @Test
    public void duplicatesKeepTheLaterEntry() {
        ByteWriter parts = new ByteWriter();
        for (String path : List.of("freestyle_5", "stack_30", "m2207_1750", "p51_3b", "analog_race", "b6s_1300")) {
            parts.varint(1).str(path);
        }
        parts.u8(2).u8(0).varint(1).str("action_cam_light").u8(0).varint(1).str("action_cam_heavy");
        byte[] tune = new ByteWriter().varint(2).varint(2 << 1).zigzag(1000).varint(2 << 1).zigzag(1500).toByteArray();
        ShareDecoded decoded = decodeOffline(frame(1, 0, payload(1 | 4, parts.toByteArray(), tune)));
        assertEquals(List.of("accessory_slot_duplicate:0", "tune_duplicate:rates.roll.rcRate"), decoded.skipped());
        assertEquals(Optional.of(id("propwash:action_cam_heavy")), decoded.require().build().accessory(AccessorySlot.TOP));
        assertEquals(1.5F, decoded.require().tune().orElseThrow().values().get("rates.roll.rcRate"));
    }

    @Test
    public void aliasesRenameOldIds() {
        Identifier old = id("justmoreparts:old_frame");
        Identifier renamed = id("justmoreparts:deadcat_5");
        BuildSpec build = buildA().withPart(PartKind.FRAME, old);
        String code = ShareCodec.encodePw1(ShareContent.of(build));
        ShareDecoded decoded = ShareCodec.decode(code, ShareCodec.PartLookup.NONE, Map.of(old, renamed));
        assertEquals(renamed, decoded.require().build().frame());
        assertEquals(List.of(new PartAliases.Rename(old, renamed)), decoded.renamed());
        assertTrue(PartAliases.all().isEmpty());
    }

    @Test
    public void encoderKeepsOnlyFlightTuningCameraAngleAndExactFloats() {
        Map<String, Float> values = new LinkedHashMap<>();
        values.put("vtx.channel", 5.0F);
        values.put("led.mode", 2.0F);
        values.put("camera.uptilt", 30.0F);
        values.put("hold.altitude", 1.0F);
        values.put("rates.roll.rcRate", -0.0F);
        values.put("pid.roll.p", 0.005F);
        values.put("pid.yaw.i", 1.0E30F);
        values.put("pid.zzz", 0.1234567F);
        ShareContent content = new ShareContent(buildA(), Optional.empty(), Optional.of(new TuneSpec(values)), Optional.empty(), Optional.empty());
        ShareDecoded decoded = decodeOffline(ShareCodec.encodePw1(content, ShareCompression.NEVER));
        Map<String, Float> back = decoded.require().tune().orElseThrow().values();
        assertEquals(5, back.size());
        assertEquals(30.0F, back.get("camera.uptilt"));
        assertFalse(back.containsKey("vtx.channel") || back.containsKey("led.mode") || back.containsKey("hold.altitude"));
        assertEquals(Float.floatToRawIntBits(-0.0F), Float.floatToRawIntBits(back.get("rates.roll.rcRate")));
        assertEquals(0.005F, back.get("pid.roll.p"));
        assertEquals(1.0E30F, back.get("pid.yaw.i"));
        assertEquals(0.1234567F, back.get("pid.zzz"));
        for (int step = -20000; step <= 20000; step++) {
            float value = (float) (step * 5 / 1000.0);
            assertEquals(step * 5L, ShareCodec.scaled(value), "Schritt " + value);
        }
        assertEquals(Long.MIN_VALUE, ShareCodec.scaled(-0.0F));
        assertEquals(Long.MIN_VALUE, ShareCodec.scaled(Float.MAX_VALUE));
    }

    @Test
    public void emptyPaintIsNotWrittenButEmptyTuneIs() {
        ShareContent content = new ShareContent(buildA(), Optional.of(PaintSpec.DEFAULT), Optional.of(TuneSpec.EMPTY), Optional.empty(),
                Optional.of("  "));
        assertEquals(EnumSet.of(ShareLayer.PARTS, ShareLayer.TUNE), content.layers());
        ShareDecoded decoded = decodeOffline(ShareCodec.encodePw1(content));
        assertEquals(List.of(ShareLayer.PARTS, ShareLayer.TUNE), decoded.layers());
    }

    @Test
    public void encoderRejectsIdsThatPw1CannotCarry() {
        BuildSpec slash = buildA().withPart(PartKind.FRAME, id("propwash:frame/x"));
        assertThrows(IllegalArgumentException.class, () -> ShareCodec.encodePw1(ShareContent.of(slash)));
    }

    @Test
    public void randomContentRoundTripsInAllModes() {
        Random random = new Random(1);
        List<String> paths = List.of("freestyle_5", "whoop_65", "deadcat_5", "m2207_1750", "a", "x.y-z_0", "p".repeat(128));
        List<String> namespaces = List.of("propwash", "justmoreparts", "otheraddon", "a.b-c_d");
        for (int round = 0; round < 400; round++) {
            Identifier[] six = new Identifier[6];
            for (int i = 0; i < 6; i++) {
                six[i] = Identifier.fromNamespaceAndPath(namespaces.get(random.nextInt(namespaces.size())), paths.get(random.nextInt(paths.size())));
            }
            EnumMap<AccessorySlot, Identifier> accessories = new EnumMap<>(AccessorySlot.class);
            for (AccessorySlot slot : AccessorySlot.values()) {
                if (random.nextBoolean()) {
                    accessories.put(slot, Identifier.fromNamespaceAndPath(namespaces.get(random.nextInt(namespaces.size())), "acc_" + random.nextInt(9)));
                }
            }
            BuildSpec build = new BuildSpec(six[0], six[1], six[2], six[3], six[4], six[5], accessories);
            PaintSpec paint = PaintSpec.DEFAULT;
            for (String slot : PaintSpec.SLOTS) {
                if (random.nextInt(3) == 0) {
                    paint = paint.with(slot, random.nextInt(0x1000000));
                }
            }
            Map<String, Float> tune = new TreeMap<>();
            int entries = random.nextInt(60);
            for (int i = 0; i < entries; i++) {
                String key = random.nextInt(4) == 0 ? "pid.custom" + random.nextInt(100) : ShareTables.TUNE_KEYS.get(random.nextInt(55));
                float value = switch (random.nextInt(3)) {
                    case 0 -> (float) (random.nextInt(4001) * 5 / 1000.0);
                    case 1 -> random.nextFloat() * 1000.0F - 500.0F;
                    default -> (float) random.nextInt(2000);
                };
                tune.put(key, value);
            }
            Optional<TuneSpec> tuneSpec = random.nextBoolean() ? Optional.of(new TuneSpec(tune)) : Optional.empty();
            Optional<ShareOsd> osd = random.nextInt(4) == 0 ? Optional.of(new ShareOsd(1 + random.nextInt(3), "{\"v\":1,\"x\":\"ä€😀" + round + "\"}"))
                    : Optional.empty();
            Optional<String> name = random.nextBoolean() ? Optional.of("Drohne Nr. " + round + " – ßü") : Optional.empty();
            ShareContent content = new ShareContent(build, random.nextBoolean() ? Optional.of(paint) : Optional.empty(), tuneSpec, osd, name);
            for (ShareCompression mode : ShareCompression.values()) {
                String code = ShareCodec.encodePw1(content, mode);
                ShareDecoded decoded = decodeOffline(code);
                assertEquals(ShareStatus.OK, decoded.status(), code);
                assertEquals(content, decoded.require(), code);
                assertEquals(List.copyOf(content.layers()), decoded.layers());
                assertTrue(code.startsWith(ShareTables.PREFIX));
            }
        }
    }

    @Test
    public void randomByteErrorsNeverThrow() {
        Random random = new Random(7);
        List<byte[]> seeds = new ArrayList<>();
        for (String code : List.of(A, B, C, D, E)) {
            seeds.add(rawFrame(code));
        }
        int ok = 0;
        for (int round = 0; round < 20000; round++) {
            byte[] frame = seeds.get(random.nextInt(seeds.size())).clone();
            int body = frame.length - 4;
            int flips = 1 + random.nextInt(4);
            for (int i = 0; i < flips; i++) {
                int at = 2 + random.nextInt(body - 2);
                frame[at] = (byte) (random.nextInt(4) == 0 ? random.nextInt(256) : frame[at] ^ (1 << random.nextInt(8)));
            }
            if (random.nextInt(5) == 0) {
                int cut = 2 + random.nextInt(body - 2);
                byte[] shorter = new byte[cut + 4];
                System.arraycopy(frame, 0, shorter, 0, cut);
                frame = shorter;
                body = cut;
            }
            String code = withCrc(frame, body);
            ShareDecoded decoded = decodeOffline(code);
            assertNotNull(decoded);
            assertNotEquals(ShareStatus.CHECKSUM, decoded.status());
            if (decoded.ok()) {
                ok++;
                assertNotNull(ShareCodec.encodePw1(decoded.require()));
            }
        }
        assertTrue(ok > 0);
        for (int round = 0; round < 5000; round++) {
            StringBuilder text = new StringBuilder("PW1-");
            int length = random.nextInt(300);
            for (int i = 0; i < length; i++) {
                text.append(ShareTables.ALPHABET.charAt(random.nextInt(32)));
            }
            ShareDecoded decoded = decodeOffline(text.toString());
            assertNotNull(decoded);
        }
    }

    @Test
    public void codeLengthsStayInsideTheLimits() {
        assertTrue(ShareCodec.fits(ShareCodec.encodePw1(contentC())));
        assertTrue(A.length() < 160);
        assertTrue(B.length() < 400);
    }

    @Test
    public void sharedBuildsOfTheRegistryAreKnown() {
        PartFiles.registerAll();
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.FRAME)) {
            BuildSpec build = buildA().withPart(PartKind.FRAME, definition.id());
            ShareDecoded decoded = ShareCodec.decode(ShareCodec.encodePw1(ShareContent.of(build)));
            assertTrue(decoded.partsKnown(), definition.id().toString());
        }
    }

    static byte[] partsBlock(BuildSpec build) {
        ByteWriter out = new ByteWriter();
        for (Identifier part : List.of(build.frame(), build.stack(), build.motor(), build.prop(), build.video(), build.battery())) {
            out.varint(ShareTables.NAMESPACES.indexOf(part.getNamespace())).str(part.getPath());
        }
        return out.u8(0).toByteArray();
    }

    static byte[] payload(int mask, byte[]... blocks) {
        ByteWriter out = new ByteWriter().varint(mask);
        for (byte[] block : blocks) {
            out.block(block);
        }
        return out.toByteArray();
    }

    static String frame(int version, int flags, byte[] stored) {
        byte[] head = new ByteWriter().u8(version).u8(flags).bytes(stored).toByteArray();
        CRC32 crc = new CRC32();
        crc.update(head);
        byte[] frame = new ByteWriter().bytes(head).u32(crc.getValue()).toByteArray();
        return ShareTables.PREFIX + Base32.group(Base32.encode(frame));
    }

    static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(9, true);
        deflater.setInput(data);
        deflater.finish();
        byte[] buffer = new byte[data.length + 64];
        int n = deflater.deflate(buffer);
        deflater.end();
        return Arrays.copyOf(buffer, n);
    }

    static byte[] rawFrame(String code) {
        try {
            return Base32.decode(code.substring(3).replace("-", ""));
        } catch (ShareFormatException e) {
            throw new IllegalStateException(e);
        }
    }

    static String withCrc(byte[] frame, int body) {
        CRC32 crc = new CRC32();
        crc.update(frame, 0, body);
        long value = crc.getValue();
        frame[body] = (byte) (value >>> 24);
        frame[body + 1] = (byte) (value >>> 16);
        frame[body + 2] = (byte) (value >>> 8);
        frame[body + 3] = (byte) value;
        return ShareTables.PREFIX + Base32.group(Base32.encode(frame));
    }

    @Test
    public void base32MatchesTheBitOrder() throws ShareFormatException {
        assertEquals("", Base32.encode(new byte[0]));
        assertEquals("04", Base32.encode(new byte[]{0x01}));
        assertEquals("ZW", Base32.encode(new byte[]{(byte) 0xFF}));
        assertArrayEquals(new byte[]{0x01, 0x00}, Base32.decode("0400"));
        assertEquals("ABCDE-FGH", Base32.group("ABCDEFGH"));
    }
}
