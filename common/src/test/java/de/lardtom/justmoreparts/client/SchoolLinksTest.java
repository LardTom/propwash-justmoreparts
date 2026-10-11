package de.lardtom.justmoreparts.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.bench.PartFiles;
import de.lardtom.justmoreparts.frame.client.FrameShapesScene;
import de.lardtom.justmoreparts.share.client.ShareScene;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.client.FlightSchoolScenes;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.guide.scene.Scene;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchoolLinksTest {
    private static final Set<String> OWN = Set.of(FrameShapesScene.ID, ShareScene.ID);

    @Test
    public void everyJmpPartKindHasFittingScenes() {
        Set<PartKind> kinds = EnumSet.noneOf(PartKind.class);
        for (Object value : PartFiles.parseAll().values()) {
            if (value instanceof PartDefinition definition) {
                kinds.add(definition.kind());
            }
        }
        assertEquals(EnumSet.complementOf(EnumSet.of(PartKind.ACCESSORY)), kinds);
        for (PartKind kind : PartKind.values()) {
            List<String> scenes = SchoolLinks.scenes(kind);
            assertFalse(scenes.isEmpty(), kind.key());
            assertEquals(FlightSchoolScenes.DRONE_BUILD, scenes.getFirst(), kind.key());
            assertTrue(scenes.contains(FlightSchoolScenes.SURVIVAL), kind.key());
            assertEquals(scenes.size(), Set.copyOf(scenes).size(), kind.key());
            for (String scene : scenes) {
                assertTrue(FlightSchoolScenes.ALL.contains(scene) || OWN.contains(scene), kind.key() + ": " + scene);
            }
        }
        assertTrue(SchoolLinks.scenes(PartKind.PROP).contains(FlightSchoolScenes.REPAIR));
        assertTrue(SchoolLinks.scenes(PartKind.MOTOR).contains(FlightSchoolScenes.REPAIR));
        assertTrue(SchoolLinks.scenes(PartKind.BATTERY).contains(FlightSchoolScenes.BATTERY));
        assertTrue(SchoolLinks.scenes(PartKind.VIDEO).contains(FlightSchoolScenes.VIDEO));
        assertTrue(SchoolLinks.scenes(PartKind.STACK).contains(FlightSchoolScenes.CONFIGURATOR));
        assertTrue(SchoolLinks.scenes(PartKind.FRAME).contains(FrameShapesScene.ID));
        assertTrue(SchoolLinks.PRESETS.contains(ShareScene.ID));
    }

    @Test
    public void linksPerSceneMatchTheTable() {
        Map<String, List<PartKind>> byScene = SchoolLinks.byScene();
        int links = 0;
        for (Map.Entry<String, List<PartKind>> entry : byScene.entrySet()) {
            for (PartKind kind : entry.getValue()) {
                assertTrue(SchoolLinks.scenes(kind).contains(entry.getKey()), entry.getKey() + " " + kind);
                links++;
            }
        }
        int expected = 0;
        for (PartKind kind : PartKind.values()) {
            expected += SchoolLinks.scenes(kind).size();
        }
        assertEquals(expected, links);
        assertEquals(EnumSet.allOf(PartKind.class), EnumSet.copyOf(byScene.get(FlightSchoolScenes.DRONE_BUILD)));
        assertEquals(List.of(PartKind.FRAME), byScene.get(FrameShapesScene.ID));
    }

    @Test
    public void ownScenesUseTheJmpNamespaceAndPresetsReachTheShareScene() {
        PartFiles.registerAll();
        assertEquals(JustMoreParts.MOD_ID + ":frames", FrameShapesScene.ID);
        assertEquals(JustMoreParts.MOD_ID + ":sharing", ShareScene.ID);
        Scene frames = FrameShapesScene.create();
        assertEquals(Set.of(JustMoreParts.id("deadcat_5").toString()), frames.items().ids());
        assertTrue(PropwashApi.parts().get(JustMoreParts.id("deadcat_5")).isPresent());
        Scene share = ShareScene.create();
        assertTrue(share.items().ids().contains("propwash:drone"), share.items().ids().toString());
        assertTrue(FlightSchoolScenes.ALL.contains(FlightSchoolScenes.DRONE_BUILD));
    }
}
