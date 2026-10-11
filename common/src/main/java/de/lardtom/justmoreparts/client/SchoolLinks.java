package de.lardtom.justmoreparts.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.frame.client.FrameShapesScene;
import de.lardtom.justmoreparts.share.client.ShareScene;
import de.lardtom.propwash.api.client.FlightSchoolScenes;
import de.lardtom.propwash.api.client.PropwashClientApi;
import de.lardtom.propwash.api.part.PartKind;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SchoolLinks {
    public static final List<String> PRESETS = List.of(FlightSchoolScenes.DRONE_BUILD, ShareScene.ID);
    private static final Map<PartKind, List<String>> BY_KIND = new EnumMap<>(PartKind.class);
    private static boolean initialized;

    static {
        BY_KIND.put(PartKind.FRAME, List.of(FlightSchoolScenes.DRONE_BUILD, FrameShapesScene.ID, FlightSchoolScenes.SURVIVAL));
        BY_KIND.put(PartKind.STACK, List.of(FlightSchoolScenes.DRONE_BUILD, FlightSchoolScenes.CONFIGURATOR, FlightSchoolScenes.SURVIVAL));
        BY_KIND.put(PartKind.MOTOR, List.of(FlightSchoolScenes.DRONE_BUILD, FlightSchoolScenes.REPAIR, FlightSchoolScenes.SURVIVAL));
        BY_KIND.put(PartKind.PROP, List.of(FlightSchoolScenes.DRONE_BUILD, FlightSchoolScenes.REPAIR, FlightSchoolScenes.SURVIVAL));
        BY_KIND.put(PartKind.VIDEO, List.of(FlightSchoolScenes.DRONE_BUILD, FlightSchoolScenes.VIDEO, FlightSchoolScenes.SURVIVAL));
        BY_KIND.put(PartKind.BATTERY, List.of(FlightSchoolScenes.DRONE_BUILD, FlightSchoolScenes.BATTERY, FlightSchoolScenes.SURVIVAL));
        BY_KIND.put(PartKind.ACCESSORY, List.of(FlightSchoolScenes.DRONE_BUILD, FlightSchoolScenes.SURVIVAL));
    }

    private SchoolLinks() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        for (Map.Entry<String, List<PartKind>> entry : byScene().entrySet()) {
            PropwashClientApi.linkFlightSchoolParts(entry.getKey(), JustMoreParts.MOD_ID, entry.getValue().toArray(PartKind[]::new));
        }
    }

    public static List<String> scenes(PartKind kind) {
        return BY_KIND.getOrDefault(kind, List.of());
    }

    public static Map<String, List<PartKind>> byScene() {
        Map<String, List<PartKind>> scenes = new LinkedHashMap<>();
        for (Map.Entry<PartKind, List<String>> entry : BY_KIND.entrySet()) {
            for (String scene : entry.getValue()) {
                scenes.computeIfAbsent(scene, key -> new ArrayList<>()).add(entry.getKey());
            }
        }
        return scenes;
    }
}
