package de.lardtom.justmoreparts.frame;

import de.lardtom.justmoreparts.JustMoreParts;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class FrameModule {
    public static final List<String> FRAMES = List.of("whoop_65_lite", "whoop_75_duct", "whoop_85_duct", "tp_2_x", "tp_25_x", "tp_3_x",
            "tp_35_stretched", "cw_25_duct", "cw_35_duct", "fs_5_bash", "fs_5_squashed", "race_5_stretched", "deadcat_5", "hframe_5",
            "lr_4_x", "lr_6_x", "lr_7_deadcat", "lr_8_x", "xclass_10", "xclass_12", "xclass_13", "xclass_13_h");
    public static final List<String> X_CLASS = List.of("xclass_10", "xclass_12", "xclass_13", "xclass_13_h");

    private FrameModule() {
    }

    public static List<Identifier> frameIds() {
        return FRAMES.stream().map(JustMoreParts::id).toList();
    }
}
