package de.lardtom.justmoreparts.share;

import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class ShareVectors {
    public static final String A = "PW1-04002-GR11D-K74SB-5EDT7-JV35B-WTG22-3KEHG-P6TTZ-6CR02-2KD68-S30DT-Z64VK-AC010-SR3AC-AZ6DH-022V1-DSGPR-VV7BX"
            + "-S62RV-50446-4DKKB-WRK6C-1G000-H23NQ";
    public static final String B = "PW1-0401E-SG215-J6ARB-4CDGQ-8QSN0-87Q6X-31CDN-NYCSG-BWV76-QSP61-GG42V-D68S3-0DSNB-WRKGD-9G086-Q0C9K-61FK6"
            + "-DJZ6D-H5YRK-90876-8TB7D-5T62V-2ZD1J-5YW3J-DW0GG-RHPED-FK2CS-G600G-008GC-5HQ8T-BFDSF-P6RBD-BXP6J-SV8EG-A00Z8-V3MHE-HDH2Z-XX1NZ"
            + "-VT3BZ-QM6QZ-F8D1M-1R4W0-911R0-J3JT1-0BM0X-R2NHY-2NBCZ-EKR23-D2GC4-7GD1H-26ARB-4CDGQ-882MC-NSQ8M-TPZ1K-0";
    public static final String C = "PW1-040S6-3V2WA-P4HKJ-95GQ8-WDSMC-R92GB-J99K7-8WDSP-H2VG1-WGD1G-99JE3-K9P60-RKF29-M60S6-041E6-824Q6-0VRMC"
            + "-JFHS6-4MAJF-SF565-RACSX-6CJB2-9SY4W-MAK0W-9503V-9G60R-30R74-CHG50-M2QTZ-7BQH6-DS38Q-F7W8W-B1J1S-MQGJ7-2SQMD-KBAMM-SJEAA-13TK8"
            + "-18PMW-BAH69-9AMJG-C9SAN-4MEA9-N4WTK-EDDFM-HCCKA-JTN6W-20D9N-0Q2CH-KTKP8-N2JAJ-DTN8M-NKPF0-5NJQJ-HF0Z9-4DDDN-PQE5R-4DTSR-G5EAR"
            + "-R381G-01PT8-BDG";
    public static final String D = "PW1-0400A-JG019-QQ8T3-5E9GP-8S3FD-R3PCW-K1DNJ-NYY01-11SQ8-RB3DD-FK6C0-119PK-4CHG6-XFK2D-SN600-GCW1N-65FK6"
            + "-RG11D-GPWRB-CDXKN-YWK1C-DJG22-326SS-NYC9K-60R00-6023K-80Y00-GE1MP-8BKEC-NVMCV-39CXM-78JV5-F7R2W-8RFB6-G0";
    public static final String E = "PW1-0404E-KR11D-K74SB-5EDT7-JV35B-WTG22-3KEHG-P6TTZ-6CR02-2KD68-S30DT-Z64VK-AC010-SR3AC-AZ6DH-022V1-DSGPR"
            + "-VV7BX-S62RV-50446-4DKKB-WRK6C-1G042-G22BC-CNJ5Y-WVME9-MQ021-0048J-4CT4A-NK1E0-WG0ER-6T00B-EST7G-BK3D1-GPWVK-5DJ84-WM6G1-W1G20"
            + "-G3RA7-XN5R";
    public static final String F = "PW1-04002-1R11D-K74SB-5EDT7-JV35B-WTG22-3KEHG-P6TTZ-6CR02-2KD68-S30DT-Z64VK-AC010-SR3AC-AZ6DH-022V1-DSGPR-VV7BX"
            + "-S62RV-50446-4DKKB-WRK6C-1G000-H23NQ";
    public static final String G = "PW1-0400A-FG10S-S62RV-5BWTG-223KE-HGP6T-TZ6CR-022KD-68SK0-DJZ64-W3CC0-10SR3-ACAZ6-DH022-V1DSG-PRVV7-BXS62-RV504"
            + "-464DK-KBWRK-2C1G0-04041-4R25R-C1B06-GN0X9-4R";
    public static final String C_OSD = "{\"v\":1,\"preset\":\"race\",\"elements\":{\"speed\":{\"on\":true,\"col\":1,\"row\":12}}}";
    public static final String P2_A = "PW205zwQBrAS4";
    public static final String P2_B = "PW26Nnq88PGI91DMWZEsVC2rZlP7Bq2a9GosBZmpxkOa4zvubUYHqScfmnw6XhX43iVl";
    public static final String P2_C = "PW2uLVHj7meSHdrh88LLf3TuzFlEZTUFxEigwazLwPa5InMLPmKpjlQwt7pvE5c4iuK5S2"
            + "z4kAvaWLT0674M5PXhw";
    public static final String P2_D = "PW20Nc8d2lmeWWcSEiZHqH5Es7RykzSmJYg3uVnhNkQuJGLL8ALqO9gBj2r";
    public static final String P2_E = "PW23bHSN9mtpDv0SxAlRviw2Jx8gpfHXadF9WUchzdWVlstEpnQM";
    public static final String P2_F = "PW205zwQCrAS4";
    public static final String P2_G = "PW20RpivqjKjr1ou9A7H";
    public static final String P2_H = "PW2TPGtbsa8PPWbpyBqdavwtUNs5zokdIOghALU6lBT5XPd0NPXmraNWX2tY5jsDqxYILCYRHeLfo6UH1ize5qin"
            + "H3gMTMqo77OjRFNa8pobzO2L2Axm";
    public static final String P2_I = "PW2CU7GUU5V0lfc9cjIDJ5XFPDDD8dpWh4NqV63buP9gYY";
    public static final List<String> P2_E_SKIPPED = List.of("part_index:frame.200", "accessory_slot:5", "paint_slot:13", "tune_index:200",
            "tune_key:vtx.channel", "layer:6");
    public static final List<String> E_SKIPPED = List.of("accessory_slot:5", "paint_slot:13", "tune_index:200", "tune_key:vtx.channel", "layer:6");

    private ShareVectors() {
    }

    public record Vector(String name, String code, ShareCompression compression, ShareStatus status, Optional<ShareContent> content,
                         List<ShareLayer> layers, List<String> skipped, String format) {
        public Vector(String name, String code, ShareCompression compression, ShareStatus status, Optional<ShareContent> content,
                      List<ShareLayer> layers, List<String> skipped) {
            this(name, code, compression, status, content, layers, skipped, ShareTables.FORMAT);
        }
    }

    public static List<Vector> all() {
        List<Vector> out = new ArrayList<>(pw1());
        out.addAll(pw2());
        return List.copyOf(out);
    }

    public static List<Vector> pw2() {
        return List.of(
                pw2("PW2_A_parts_only", P2_A, ShareStatus.OK, Optional.of(contentA()), List.of(ShareLayer.PARTS), List.of()),
                pw2("PW2_B_parts_paint_tune_name", P2_B, ShareStatus.OK, Optional.of(contentB()),
                        List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.NAME), List.of()),
                pw2("PW2_C_all_layers_osd", P2_C, ShareStatus.OK, Optional.of(contentP2C()),
                        List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.OSD, ShareLayer.NAME), List.of()),
                pw2("PW2_D_foreign_namespace", P2_D, ShareStatus.OK, Optional.of(contentD()), List.of(ShareLayer.PARTS, ShareLayer.TUNE), List.of()),
                pw2("PW2_E_tolerant_skips", P2_E, ShareStatus.OK, Optional.of(contentP2E()), List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE),
                        P2_E_SKIPPED),
                pw2("PW2_F_checksum_error", P2_F, ShareStatus.CHECKSUM, Optional.empty(), List.of(), List.of()),
                pw2("PW2_G_camera_angle", P2_G, ShareStatus.OK, Optional.of(contentG()), List.of(ShareLayer.PARTS, ShareLayer.TUNE), List.of()),
                pw2("PW2_H_text_colors_values", P2_H, ShareStatus.OK, Optional.of(contentP2H()),
                        List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.NAME), List.of()),
                pw2("PW2_I_utf8_name_short_osd", P2_I, ShareStatus.OK, Optional.of(contentP2I()),
                        List.of(ShareLayer.PARTS, ShareLayer.OSD, ShareLayer.NAME), List.of()));
    }

    private static Vector pw2(String name, String code, ShareStatus status, Optional<ShareContent> content, List<ShareLayer> layers,
                              List<String> skipped) {
        return new Vector(name, code, ShareCompression.AUTO, status, content, layers, skipped, Pw2Tables.FORMAT);
    }

    public static List<Vector> pw1() {
        return List.of(
                new Vector("A_parts_only", A, ShareCompression.NEVER, ShareStatus.OK, Optional.of(contentA()), List.of(ShareLayer.PARTS), List.of()),
                new Vector("B_parts_paint_tune_name", B, ShareCompression.NEVER, ShareStatus.OK, Optional.of(contentB()),
                        List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.NAME), List.of()),
                new Vector("C_all_layers_compressed", C, ShareCompression.ALWAYS, ShareStatus.OK, Optional.of(contentC()),
                        List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE, ShareLayer.OSD, ShareLayer.NAME), List.of()),
                new Vector("D_foreign_namespace", D, ShareCompression.NEVER, ShareStatus.OK, Optional.of(contentD()),
                        List.of(ShareLayer.PARTS, ShareLayer.TUNE), List.of()),
                new Vector("E_tolerant_skips", E, ShareCompression.NEVER, ShareStatus.OK, Optional.of(contentE()),
                        List.of(ShareLayer.PARTS, ShareLayer.PAINT, ShareLayer.TUNE), E_SKIPPED),
                new Vector("F_checksum_error", F, ShareCompression.NEVER, ShareStatus.CHECKSUM, Optional.empty(), List.of(), List.of()),
                new Vector("G_camera_angle", G, ShareCompression.NEVER, ShareStatus.OK, Optional.of(contentG()),
                        List.of(ShareLayer.PARTS, ShareLayer.TUNE), List.of()));
    }

    private static Identifier id(String text) {
        return Identifier.parse(text);
    }

    public static BuildSpec buildA() {
        return BuildSpec.of(id("propwash:freestyle_5"), id("propwash:stack_30"), id("propwash:m2207_1750"), id("propwash:p51_3b"),
                id("propwash:analog_race"), id("propwash:b6s_1300"));
    }

    public static ShareContent contentA() {
        return ShareContent.of(buildA());
    }

    public static ShareContent contentB() {
        BuildSpec build = BuildSpec.of(id("justmoreparts:deadcat_5"), id("justmoreparts:stack_30_6s_60a"), id("justmoreparts:m22075_1850"),
                        id("justmoreparts:p130_36_3b_bi"), id("justmoreparts:digital_hd_pro"), id("propwash:b6s_1300"))
                .withAccessory(AccessorySlot.TOP, id("propwash:action_cam_light"));
        PaintSpec paint = PaintSpec.DEFAULT.with("frame", 0x1B1D22).with("motors", 0xE8B622).with("prop_fl", 0xFF7A1A).with("prop_fr", 0xFF7A1A)
                .with("prop_rl", 0xFF7A1A).with("prop_rr", 0xFF7A1A);
        Map<String, Float> tune = new TreeMap<>();
        tune.put("rates.roll.rcRate", 1.2F);
        tune.put("rates.pitch.rcRate", 1.2F);
        tune.put("rates.yaw.rcRate", 1.05F);
        tune.put("pid.roll.p", 48.0F);
        tune.put("pid.throttleMid", 0.4567891F);
        tune.put("pid.motorIdlePercent", 5.5F);
        tune.put("filters.gyroLpfHz", 250.0F);
        return new ShareContent(build, Optional.of(paint), Optional.of(new TuneSpec(tune)), Optional.empty(), Optional.of("Deadcat Test"));
    }

    public static ShareContent contentC() {
        BuildSpec build = BuildSpec.of(id("justmoreparts:xclass_13"), id("justmoreparts:stack_30_8s_100a"), id("justmoreparts:m4214_400"),
                id("justmoreparts:p330_70_2b_carbon"), id("justmoreparts:digital_lr"), id("justmoreparts:b8s_5000"));
        PaintSpec paint = PaintSpec.DEFAULT.with("frame", 0x202020).with("battery", 0x2F6FD6);
        TuneSpec tune = new TuneSpec(Map.of("rates.roll.centerRateDps", 120.0F, "rates.roll.maxRateDps", 420.0F, "pid.tpaRatePercent", 40.0F));
        return new ShareContent(build, Optional.of(paint), Optional.of(tune), Optional.of(new ShareOsd(1, C_OSD)), Optional.of("X-Class 13"));
    }

    public static ShareContent contentD() {
        BuildSpec build = BuildSpec.of(id("otheraddon:frame_x"), id("propwash:stack_30"), id("propwash:m2207_1750"), id("propwash:p51_3b"),
                id("propwash:analog_race"), id("propwash:b6s_1300"));
        TuneSpec tune = new TuneSpec(Map.of("rates.yaw.rcRate", 1.0F, "pid.newFlightKey", 3.0F));
        return new ShareContent(build, Optional.empty(), Optional.of(tune), Optional.empty(), Optional.empty());
    }

    public static ShareContent contentE() {
        return new ShareContent(buildA(), Optional.of(PaintSpec.DEFAULT.with("frame", 0x112233)),
                Optional.of(new TuneSpec(Map.of("pid.airmode", 1.0F))), Optional.empty(), Optional.empty());
    }

    public static String osdP2C() {
        return Pw2Tables.osdPreset("race").replace("\"speed\":{\"on\":false", "\"speed\":{\"on\":true").replace("\"scale\":1.0,", "\"scale\":1.15,");
    }

    public static ShareContent contentP2C() {
        ShareContent c = contentC();
        return new ShareContent(c.build(), c.paint(), c.tune(), Optional.of(new ShareOsd(1, osdP2C())), c.name());
    }

    public static ShareContent contentP2E() {
        BuildSpec build = buildA().withPart(PartKind.FRAME, Identifier.fromNamespaceAndPath(Pw2Tables.UNKNOWN_NAMESPACE, "frame.200"));
        return new ShareContent(build, Optional.of(PaintSpec.DEFAULT.with("frame", 0x112233)),
                Optional.of(new TuneSpec(Map.of("pid.airmode", 1.0F))), Optional.empty(), Optional.empty());
    }

    public static ShareContent contentP2H() {
        BuildSpec build = BuildSpec.of(id("justmoreparts:hframe_5"), id("justmoreparts:stack_30_6s_80a"), id("justmoreparts:m2207_1950"),
                        id("justmoreparts:p51_20_3b_pc"), id("propwash:digital_lite"), id("justmoreparts:b6s_1800"))
                .withAccessory(AccessorySlot.TOP, id("propwash:action_cam_heavy")).withAccessory(AccessorySlot.BOTTOM, id("propwash:led_strip"));
        PaintSpec paint = PaintSpec.DEFAULT;
        int[] colors = {0x2A2C30, 0x123456, 0xF9801D, 0xFED83D, 0xFED83D, 0x123456, 0x123456, 0x000000, 0xFFFFFF, 0x2A2C30, 0xB02E26, 0xF9801D};
        for (int i = 0; i < colors.length; i++) {
            paint = paint.with(PaintSpec.SLOTS.get(i), colors[i]);
        }
        Map<String, Float> tune = new TreeMap<>();
        tune.put("rates.type", 1.0F);
        tune.put("rates.roll.centerRateDps", 200.0F);
        tune.put("rates.pitch.centerRateDps", 200.0F);
        tune.put("rates.roll.maxRateDps", 670.0F);
        tune.put("rates.roll.actualExpo", 0.54F);
        tune.put("rates.rcDeadband", 0.015F);
        tune.put("pid.motorIdlePercent", 5.5F);
        tune.put("pid.yaw.p", 0.1234567F);
        tune.put("pid.roll.ff", -12.0F);
        tune.put("pid.throttleMid", 1.0F / 3.0F);
        tune.put("pid.armMaxAngleDeg", 45.0F);
        tune.put("pid.someNewKey", 0.54F);
        tune.put(TuneSpec.CAMERA_UPTILT, 32.5F);
        return new ShareContent(build, Optional.of(paint), Optional.of(new TuneSpec(tune)), Optional.empty(), Optional.of("Race #1 (v2)"));
    }

    public static ShareContent contentP2I() {
        return new ShareContent(buildA(), Optional.empty(), Optional.empty(), Optional.of(new ShareOsd(3, "\u00e4\u20ac")),
                Optional.of("Flitzer Ä 🚀 ß"));
    }

    public static ShareContent contentG() {
        BuildSpec build = BuildSpec.of(id("propwash:race_5"), id("propwash:stack_30"), id("propwash:m2306_1860"), id("propwash:p51_3b"),
                id("propwash:analog_race"), id("propwash:b6s_1100"));
        TuneSpec tune = new TuneSpec(Map.of("rates.roll.rcRate", 1.1F, TuneSpec.CAMERA_UPTILT, 52.0F));
        return new ShareContent(build, Optional.empty(), Optional.of(tune), Optional.empty(), Optional.empty());
    }

    public static List<String> check(Vector vector, ShareDecoded decoded) {
        if (vector.format().equals(Pw2Tables.FORMAT)) {
            return checkPw2(vector, decoded);
        }
        List<String> errors = new ArrayList<>();
        if (decoded.status() != vector.status()) {
            errors.add("Status " + decoded.status() + " statt " + vector.status());
            return errors;
        }
        if (!decoded.content().equals(vector.content())) {
            errors.add("Inhalt weicht ab: " + decoded.content() + " statt " + vector.content());
        }
        if (!decoded.layers().equals(vector.layers())) {
            errors.add("Ebenen " + decoded.layers() + " statt " + vector.layers());
        }
        if (!decoded.skipped().equals(vector.skipped())) {
            errors.add("übersprungen " + decoded.skipped() + " statt " + vector.skipped());
        }
        if (vector.compression() == ShareCompression.NEVER && vector.content().isPresent() && vector.skipped().isEmpty()) {
            String again = ShareCodec.encodePw1(vector.content().get(), ShareCompression.NEVER);
            if (!again.equals(vector.code())) {
                errors.add("nicht bytegleich kodiert: " + again);
            }
        }
        return errors;
    }

    private static List<String> checkPw2(Vector vector, ShareDecoded decoded) {
        List<String> errors = new ArrayList<>();
        if (decoded.status() != vector.status()) {
            errors.add("Status " + decoded.status() + " statt " + vector.status());
            return errors;
        }
        if (!decoded.content().equals(vector.content())) {
            errors.add("Inhalt weicht ab: " + decoded.content() + " statt " + vector.content());
        }
        if (!decoded.layers().equals(vector.layers())) {
            errors.add("Ebenen " + decoded.layers() + " statt " + vector.layers());
        }
        if (!decoded.skipped().equals(vector.skipped())) {
            errors.add("übersprungen " + decoded.skipped() + " statt " + vector.skipped());
        }
        if (vector.content().isPresent() && vector.skipped().isEmpty()) {
            String again = ShareCodec.encode(vector.content().get());
            if (!again.equals(vector.code())) {
                errors.add("nicht bytegleich kodiert: " + again);
            }
        }
        return errors;
    }
}
