package de.lardtom.justmoreparts.preset;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.DronePreset;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public final class JmpPresets {
    public static final int NO_COLOR = -1;

    public static final List<Entry> ALL = List.of(
            new Entry("whoop75_1s", SizeClass.WHOOP, "whoop_75_duct", "aio_whoop_1s_6a", "m0802_19000", "p40_17_3b_pc",
                    "analog_whoop_25mw", "b1s_450_hv", 0x3FB950, NO_COLOR, 0xE6EDF3),
            new Entry("whoop85_2s", SizeClass.WHOOP, "whoop_85_duct", "aio_whoop_2s_12a", "m1102_13500", "p45_17_4b_bi",
                    "digital_micro", "b2s_450_hv", 0x2F81F7, NO_COLOR, 0xF0F6FC),
            new Entry("toothpick2_2s", SizeClass.TOOTHPICK, "tp_2_x", "aio_whoop_2s_12a", "m1103_11000", "p51_20_3b_pc",
                    "analog_whoop_25mw", "b2s_450_hv", 0x1B1D20, NO_COLOR, 0xF5C518),
            new Entry("toothpick3_4s", SizeClass.TOOTHPICK, "tp_3_x", "aio_tp_4s_20a", "m1404_3800", "p76_25_3b_hard",
                    "digital_micro", "propwash:b4s_650", 0x1B1D20, NO_COLOR, 0xA371F7),
            new Entry("toothpick35_6s", SizeClass.TOOTHPICK, "tp_35_stretched", "aio_tp_6s_20a", "m1505_2500", "p89_30_3b_pc",
                    "digital_micro", "b6s_750", 0x1B1D20, NO_COLOR, 0x19B3A6),
            new Entry("cinewhoop25_4s", SizeClass.CINEWHOOP, "cw_25_duct", "aio_tp_4s_20a", "m1404_3800", "p65_22_4b_bi",
                    "digital_micro", "b4s_850", 0x1B1D20, 0xE5534B, 0xC9D1D9),
            new Entry("cinewhoop35_6s", SizeClass.CINEWHOOP, "cw_35_duct", "aio_20_6s_45a", "m2004_1700", "p89_25_5b_pc_duct",
                    "digital_hd_pro", "b6s_850", 0x1B1D20, 0xD29922, 0x30363D),
            new Entry("bash5_6s", SizeClass.FIVE_INCH, "fs_5_bash", "stack_30_6s_60a", "m2207_1950", "p130_41_3b_soft",
                    "digital_hd_pro", "propwash:b6s_1300", 0x1B1D20, 0xCF222E, 0xCF222E),
            new Entry("squashed5_6s", SizeClass.FIVE_INCH, "fs_5_squashed", "stack_30_6s_60a", "m2207_1950", "p130_36_3b_bi",
                    "digital_hd_pro", "propwash:b6s_1300", 0x2D1B45, 0x8250DF, 0x8250DF),
            new Entry("deadcat5_6s", SizeClass.FIVE_INCH, "deadcat_5", "stack_30_6s_60a", "m22075_1850", "p130_36_3b_bi",
                    "digital_hd_pro", "propwash:b6s_1300", 0x1B1D20, 0x1A7F37, 0x2DA44E),
            new Entry("race5_4s", SizeClass.FIVE_INCH, "race_5_stretched", "stack_30_4s_55a", "m23065_2450", "p130_46_3b_hard",
                    "digital_low_latency", "b4s_1500", 0x0B3D91, 0x0969DA, 0x54AEFF),
            new Entry("race5_6s", SizeClass.FIVE_INCH, "race_5_stretched", "stack_30_6s_60a", "m23065_2020", "p130_50_3b_gfn",
                    "analog_race_800", "propwash:b6s_1300", 0x1B1D20, 0xFF7B29, 0xFF7B29),
            new Entry("hframe5_6s", SizeClass.FIVE_INCH, "hframe_5", "stack_30_6s_60a", "m2207_1950", "p130_35_4b_pc",
                    "digital_hd_pro", "propwash:b6s_1300", 0x1B1D20, 0xBF8700, 0xBF8700),
            new Entry("lr4_6s_lion", SizeClass.LONG_RANGE, "lr_4_x", "aio_20_6s_45a", "m2004_1700", "p102_30_3b_gfn",
                    "digital_micro", "b6s_3000_lion", 0x1B1D20, NO_COLOR, 0x54AEFF),
            new Entry("lr6_6s", SizeClass.LONG_RANGE, "lr_6_x", "stack_30_6s_60a", "m2507_1500", "p152_33_3b_pc",
                    "digital_lr", "b6s_2200", 0x1B1D20, 0xFB8F44, 0xFB8F44),
            new Entry("lr7_deadcat_lion", SizeClass.LONG_RANGE, "lr_7_deadcat", "stack_30_6s_60a", "m28065_1300", "p178_40_2b_cfn",
                    "digital_lr", "b6s_4500_lion", 0x1B1D20, 0x2DA44E, 0x7EE787),
            new Entry("lr7_8s", SizeClass.LONG_RANGE, "lr_7_deadcat", "stack_30_8s_65a", "m2807_1100", "p178_40_2b_cfn",
                    "digital_lr", "b8s_2200", 0x1B1D20, 0x1F6FEB, 0x79C0FF),
            new Entry("lr8_6s_lion", SizeClass.LONG_RANGE, "lr_8_x", "stack_30_8s_65a", "m2807_1100", "p203_45_2b_cfn",
                    "digital_lr", "b6s_8400_lion", 0x1B1D20, 0xE16F24, 0xE16F24),
            new Entry("xclass10_6s", SizeClass.X_CLASS, "xclass_10", "stack_30_8s_100a", "m3115_900", "p254_50_2b_carbon",
                    "digital_hd_pro", "b6s_5000", 0x1B1D20, 0x3FB950, 0x3FB950),
            new Entry("xclass12_8s", SizeClass.X_CLASS, "xclass_12", "stack_30_8s_100a", "m4214_400", "p305_60_2b_cfn",
                    "digital_lr", "b8s_5000", 0x1B1D20, 0xF778BA, 0xF778BA),
            new Entry("xclass13_8s", SizeClass.X_CLASS, "xclass_13", "stack_30_8s_100a", "m4214_400", "p330_70_2b_carbon",
                    "digital_lr", "b8s_5000", 0x1B1D20, 0xFFD33D, 0xFFD33D),
            new Entry("xclass13h_8s", SizeClass.X_CLASS, "xclass_13_h", "stack_30_8s_100a", "m4214_400", "p330_60_3b_cfn",
                    "digital_lr", "b8s_5000", 0x1B1D20, 0xFF4D4D, 0xFF4D4D));

    private JmpPresets() {
    }

    public static Identifier part(String text) {
        return text.indexOf(':') < 0 ? JustMoreParts.id(text) : Identifier.parse(text);
    }

    public static Entry byPath(String path) {
        for (Entry entry : ALL) {
            if (entry.path().equals(path)) {
                return entry;
            }
        }
        throw new IllegalArgumentException("unbekanntes Preset: " + path);
    }

    public enum SizeClass {
        WHOOP,
        TOOTHPICK,
        CINEWHOOP,
        FIVE_INCH,
        LONG_RANGE,
        X_CLASS
    }

    public record Entry(String path, SizeClass sizeClass, String frame, String stack, String motor, String prop, String video,
                        String battery, int frameColor, int tpuColor, int motorColor) {
        public Identifier id() {
            return JustMoreParts.id(this.path);
        }

        public String translationKey() {
            return JustMoreParts.MOD_ID + ".preset." + this.path;
        }

        public BuildSpec build() {
            return BuildSpec.of(part(this.frame), part(this.stack), part(this.motor), part(this.prop), part(this.video), part(this.battery));
        }

        public PaintSpec paint() {
            PaintSpec paint = PaintSpec.DEFAULT.with("frame", this.frameColor).with("motors", this.motorColor);
            return this.tpuColor == NO_COLOR ? paint : paint.with("tpu", this.tpuColor);
        }

        public DroneData data() {
            return new DroneData(this.build(), this.paint(), TuneSpec.EMPTY, Optional.empty());
        }

        public DronePreset preset() {
            return DronePreset.of(this.id(), this.data());
        }
    }
}
