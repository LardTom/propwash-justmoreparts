package de.lardtom.justmoreparts.video;

import de.lardtom.justmoreparts.JustMoreParts;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class VideoModule {
    public static final List<String> VIDEOS = List.of("analog_whoop_25mw", "analog_race_800", "analog_lr_1600", "digital_micro",
            "digital_low_latency", "digital_hd_pro", "digital_lr");

    private VideoModule() {
    }

    public static void init() {
    }

    public static List<Identifier> videoIds() {
        return VIDEOS.stream().map(JustMoreParts::id).toList();
    }
}
