package de.lardtom.justmoreparts.dev.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.client.SchoolLinks;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.frame.client.FrameShapesScene;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DronePreset;
import de.lardtom.propwash.api.client.PropwashClientApi;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.ui.client.theme.ThemedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class SchoolSelfTests {
    public static final Identifier OPENED = JustMoreParts.id("deadcat_5");
    private static final String PHASE = "phase";

    private SchoolSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep("jmp: Flugschul-Taste über jedem JMP-Item und jedem JMP-Preset öffnet passende Szenen", 200, SchoolSelfTests::step);
    }

    private static StepResult step(SelfTest.StepContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Object> state = ctx.state();
        int phase = (int) state.getOrDefault(PHASE, 0);
        if (phase == 0) {
            if (mc.gui.screen() != null) {
                mc.gui.setScreen(null);
                return StepResult.pending();
            }
            List<String> problems = check(ctx);
            if (!problems.isEmpty()) {
                return StepResult.fail(String.join("; ", problems));
            }
            ItemStack frame = PropwashApi.partItems().stack(OPENED);
            if (!PropwashClientApi.openFlightSchool(frame, null)) {
                return StepResult.fail("openFlightSchool öffnet für " + OPENED + " nichts");
            }
            state.put(PHASE, 1);
            return StepResult.pending();
        }
        Screen screen = mc.gui.screen();
        if (screen == null) {
            return ctx.tick() > 100 ? StepResult.fail("kein Flugschul-Fenster für " + OPENED) : StepResult.pending();
        }
        boolean themed = screen instanceof ThemedScreen;
        ctx.log("Flugschul-Fenster für " + OPENED + ": " + screen.getClass().getSimpleName());
        screen.onClose();
        mc.gui.setScreen(null);
        return StepResult.check(themed, "Flugschul-Fenster ohne Propwash-Theme: " + screen.getClass().getName());
    }

    public static List<String> check(SelfTest.StepContext ctx) {
        List<String> problems = new ArrayList<>();
        Map<String, Integer> counts = new TreeMap<>();
        int items = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!JustMoreParts.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            items++;
            List<String> scenes = PropwashClientApi.flightSchoolScenes(new ItemStack(item));
            if (scenes.isEmpty()) {
                problems.add(id + " öffnet keine Flugschul-Szene");
            }
            for (String scene : scenes) {
                counts.merge(scene, 1, Integer::sum);
            }
        }
        if (items == 0 || items != JustMoreParts.items()) {
            problems.add(items + " JMP-Items im Register, aber " + JustMoreParts.items() + " Teil-Items");
        }
        int parts = 0;
        for (PartDefinition definition : PropwashApi.parts().all()) {
            if (!JustMoreParts.MOD_ID.equals(definition.id().getNamespace())) {
                continue;
            }
            parts++;
            ItemStack stack = PropwashApi.partItems().stack(definition.id());
            List<String> expected = SchoolLinks.scenes(definition.kind());
            List<String> scenes = PropwashClientApi.flightSchoolScenes(stack);
            if (stack.isEmpty() || expected.isEmpty() || !scenes.containsAll(expected)) {
                List<String> missing = new ArrayList<>(expected);
                missing.removeAll(scenes);
                problems.add(definition.id() + " (" + definition.kind().key() + "): " + scenes + ", es fehlen " + missing);
            }
        }
        int presets = 0;
        for (DronePreset preset : PropwashApi.builds().presets()) {
            if (!JustMoreParts.MOD_ID.equals(preset.id().getNamespace())) {
                continue;
            }
            presets++;
            List<String> scenes = PropwashClientApi.flightSchoolScenes(PropwashApi.builds().createDrone(preset.data()));
            if (!scenes.containsAll(SchoolLinks.PRESETS)) {
                problems.add("Preset " + preset.id() + ": " + scenes + " ohne " + SchoolLinks.PRESETS);
            }
        }
        if (presets == 0) {
            problems.add("keine JMP-Presets gefunden");
        }
        long frames = PropwashApi.parts().all(PartKind.FRAME).stream().filter(d -> JustMoreParts.MOD_ID.equals(d.id().getNamespace())).count();
        int framed = counts.getOrDefault(FrameShapesScene.ID, 0);
        if (frames == 0 || framed != frames) {
            problems.add("Rahmenformen-Szene an " + framed + " Items statt an allen " + frames + " JMP-Rahmen");
        }
        ctx.log(items + " JMP-Items, " + parts + " Teile, " + presets + " Presets; Items je Szene: " + counts);
        return problems;
    }
}
