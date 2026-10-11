package de.lardtom.justmoreparts.frame.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.frame.FrameLoot;
import de.lardtom.justmoreparts.frame.FrameModule;
import de.lardtom.justmoreparts.platform.Services;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.justmoreparts.video.VideoModule;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.build.CameraView;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.DronePreset;
import de.lardtom.propwash.api.client.PropwashClientApi;
import de.lardtom.propwash.api.part.FrameDefinition;
import de.lardtom.propwash.api.part.FrameShape;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import de.lardtom.propwash.api.part.Vec3Mm;
import de.lardtom.propwash.guide.scene.Scene;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class FrameSelfTests {
    private static final String FUTURE = "future";
    private static final String RESULT = "result";
    private static final int INJECT_ROLLS = 400;
    private static final int TARGET_ROLLS = 600;
    private static final Identifier EDGE_PROPS = JustMoreParts.id("lr_7_deadcat");
    private static final double MIN_PROPS_IN_VIEW = 1.0;
    private static final double MAX_PROPS_IN_VIEW = 15.0;

    private FrameSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep("frame: 22 Rahmen und 7 Videosysteme mit Item, Name und Shift-Hilfe", 40, ctx -> {
            List<String> problems = new ArrayList<>();
            for (String path : FrameModule.FRAMES) {
                checkPart(path, PartKind.FRAME, problems);
                if (!has(JustMoreParts.MOD_ID + ".frame_short." + path)) {
                    problems.add(path + ": Kurzname fehlt");
                }
                ItemStack stack = PropwashApi.partItems().stack(JustMoreParts.id(path));
                Rarity expected = FrameModule.X_CLASS.contains(path) ? Rarity.RARE : Rarity.COMMON;
                if (!stack.isEmpty() && stack.getRarity() != expected) {
                    problems.add(path + ": Seltenheit " + stack.getRarity() + " statt " + expected);
                }
            }
            for (String path : VideoModule.VIDEOS) {
                checkPart(path, PartKind.VIDEO, problems);
            }
            return StepResult.check(problems.isEmpty(), String.join("; ", problems));
        });
        SelfTest.clientStep("frame: Deadcats und H-Frames ohne Props im FPV-Bild, Deadcat LR 7″ mit Propspitzen am Bildrand (Kamera-Aufbau der API)", 20, ctx -> {
            List<String> problems = new ArrayList<>();
            List<String> seen = new ArrayList<>();
            for (DronePreset preset : PropwashApi.builds().presets()) {
                BuildSpec spec = preset.data().build();
                if (!JustMoreParts.MOD_ID.equals(preset.id().getNamespace())
                        || !(PropwashApi.parts().get(spec.frame()).orElse(null) instanceof FrameDefinition frame)) {
                    continue;
                }
                FrameShape shape = frame.shape();
                if (shape != FrameShape.DEADCAT && shape != FrameShape.H_FRAME) {
                    continue;
                }
                Optional<CameraView> view = PropwashApi.builds().cameraView(spec);
                Optional<BuildStats> stats = PropwashApi.builds().analyze(spec);
                if (view.isEmpty() || stats.isEmpty()) {
                    problems.add(preset.id() + ": kein Kamera-Aufbau");
                    continue;
                }
                double percent = view.get().propsInViewPercent();
                boolean edges = EDGE_PROPS.equals(spec.frame());
                if (edges ? percent <= 1.0 || percent >= 3.0 : percent != 0.0) {
                    problems.add(preset.id() + ": " + percent + " % Props im FPV-Bild");
                }
                if (stats.get().propsInViewPercent() != percent) {
                    problems.add(preset.id() + ": Werkbank " + stats.get().propsInViewPercent() + " % statt " + percent + " %");
                }
                if (view.get().props().size() != 4 || frame.geometry().cameraMm().map(c -> c.z() != view.get().lensMm().z()).orElse(true)) {
                    problems.add(preset.id() + ": Objektiv weicht vom Rahmen ab");
                }
                double front = view.get().props().stream().mapToDouble(d -> d.centerMm().z()).min().orElse(0.0);
                if (!(view.get().lensMm().z() < front)) {
                    problems.add(preset.id() + ": Objektiv nicht vor den Vorderprops");
                }
                seen.add(preset.id().getPath() + " " + String.format(Locale.ROOT, "%.2f %% bei %.0f°/%.0f°", percent,
                        view.get().horizontalFovDeg(), view.get().uptiltDeg()));
            }
            if (seen.size() < 5) {
                problems.add("nur " + seen.size() + " Deadcat-/H-Presets geprüft");
            }
            ctx.log("Props im FPV-Bild: " + String.join(", ", seen));
            return StepResult.check(problems.isEmpty(), String.join("; ", problems));
        });
        SelfTest.clientStep("frame: jeder Rahmen mit Kamerahalter, Props im FPV-Bild nach Rahmenform (Kamera-Aufbau der API)", 20, ctx -> {
            List<String> problems = new ArrayList<>();
            for (String path : FrameModule.FRAMES) {
                if (!(PropwashApi.parts().get(JustMoreParts.id(path)).orElse(null) instanceof FrameDefinition frame)
                        || frame.geometry().cameraMm().isEmpty()) {
                    problems.add(path + ": kein Kamerahalter (camera.pos)");
                }
            }
            List<String> seen = new ArrayList<>();
            for (DronePreset preset : PropwashApi.builds().presets()) {
                BuildSpec spec = preset.data().build();
                if (!JustMoreParts.MOD_ID.equals(preset.id().getNamespace())
                        || !(PropwashApi.parts().get(spec.frame()).orElse(null) instanceof FrameDefinition frame)) {
                    continue;
                }
                Optional<CameraView> view = PropwashApi.builds().cameraView(spec);
                if (view.isEmpty()) {
                    problems.add(preset.id() + ": kein Kamera-Aufbau");
                    continue;
                }
                double percent = view.get().propsInViewPercent();
                boolean clean = frame.shape() == FrameShape.DEADCAT || frame.shape() == FrameShape.H_FRAME;
                if (!clean && !(percent >= MIN_PROPS_IN_VIEW && percent < MAX_PROPS_IN_VIEW)) {
                    problems.add(preset.id() + ": " + percent + " % Props im FPV-Bild statt " + MIN_PROPS_IN_VIEW + "–" + MAX_PROPS_IN_VIEW + " %");
                }
                Vec3Mm lens = view.get().lensMm();
                frame.geometry().cameraMm().filter(c -> Math.abs(c.x() - lens.x()) > 1.0E-3 || Math.abs(c.y() - lens.y()) > 1.0E-3
                        || Math.abs(c.z() - lens.z()) > 1.0E-3).ifPresent(c -> problems.add(preset.id() + ": Objektiv " + lens + " statt Kamerahalter " + c));
                seen.add(preset.id().getPath() + String.format(Locale.ROOT, " %.2f %%", percent));
            }
            Map<String, Double> shapes = new HashMap<>();
            for (FrameShapesScene.Actor actor : FrameShapesScene.actors()) {
                PropwashApi.builds().cameraView(actor.build()).ifPresent(v -> shapes.put(actor.name(), v.propsInViewPercent()));
            }
            double stretched = shapes.getOrDefault(FrameShapesScene.STRETCHED, Double.NaN);
            double truex = shapes.getOrDefault(FrameShapesScene.TRUE_X, Double.NaN);
            double squashed = shapes.getOrDefault(FrameShapesScene.SQUASHED, Double.NaN);
            double clean = Math.max(shapes.getOrDefault(FrameShapesScene.DEADCAT, Double.NaN), shapes.getOrDefault(FrameShapesScene.H_FRAME, Double.NaN));
            if (!(stretched > truex && truex > squashed && squashed > clean)) {
                problems.add("Rahmenformen mit gleichen Teilen: Stretched " + stretched + ", True-X " + truex + ", Squashed " + squashed
                        + ", Deadcat/H " + clean + " %");
            }
            ctx.log("Props im FPV-Bild: " + String.join(", ", seen) + String.format(Locale.ROOT,
                    "; gleiche Teile: Stretched %.2f, True-X %.2f, Squashed %.2f, Deadcat/H %.2f %%", stretched, truex, squashed, clean));
            return StepResult.check(problems.isEmpty(), String.join("; ", problems));
        });
        SelfTest.clientStep("frame: 5″-Formen und Videosysteme mit eingebauten Teilen baubar, Deadcat/H ohne Props im Bild", 40, ctx -> {
            List<String> problems = new ArrayList<>();
            Map<String, BuildStats> stats = new HashMap<>();
            for (FrameShapesScene.Actor actor : FrameShapesScene.actors()) {
                BuildSpec spec = actor.build();
                roundTrip(actor.name(), DroneData.of(spec), problems).ifPresent(s -> stats.put(actor.name(), s));
            }
            BuildStats deadcat = stats.get(FrameShapesScene.DEADCAT);
            BuildStats hframe = stats.get(FrameShapesScene.H_FRAME);
            BuildStats truex = stats.get(FrameShapesScene.TRUE_X);
            BuildStats stretched = stats.get(FrameShapesScene.STRETCHED);
            if (deadcat != null && deadcat.propsInViewPercent() >= 1.0) {
                problems.add("Deadcat zeigt " + deadcat.propsInViewPercent() + " % Props");
            }
            if (hframe != null && hframe.propsInViewPercent() >= 1.0) {
                problems.add("H-Frame zeigt " + hframe.propsInViewPercent() + " % Props");
            }
            if (truex != null && stretched != null && stretched.topSpeedKmh() <= truex.topSpeedKmh()) {
                problems.add("Stretched-X nicht schneller als True-X");
            }
            for (String path : VideoModule.VIDEOS) {
                BuildSpec spec = FrameShapesScene.build("propwash:freestyle_5").withPart(PartKind.VIDEO, JustMoreParts.id(path));
                roundTrip(path, DroneData.of(spec), problems);
            }
            ctx.log("Deadcat " + fmt(deadcat) + ", H-Frame " + fmt(hframe) + ", True-X " + fmt(truex) + ", Stretched " + fmt(stretched));
            return StepResult.check(problems.isEmpty(), String.join("; ", problems));
        });
        SelfTest.clientStep("frame: Flugschul-Szene „Rahmenformen“ mit Texten öffnen und schließen", 80, ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (ctx.tick() == 1) {
                List<String> problems = new ArrayList<>();
                Scene scene = FrameShapesScene.create();
                if (!FrameShapesScene.ID.equals(scene.id())) {
                    problems.add("Szenen-Id " + scene.id());
                }
                String prefix = JustMoreParts.MOD_ID + ".guide.scene.frames.";
                List<String> keys = new ArrayList<>(List.of(prefix + "title", prefix + "description"));
                for (String step : FrameShapesScene.STEPS) {
                    keys.add(prefix + "step." + step + ".title");
                    keys.add(prefix + "step." + step + ".text");
                }
                for (String callout : FrameShapesScene.CALLOUTS) {
                    keys.add(prefix + "callout." + callout);
                }
                for (String key : keys) {
                    if (!has(key)) {
                        problems.add("Text fehlt: " + key);
                    }
                }
                if (!PropwashClientApi.openFlightSchoolScene(FrameShapesScene.ID, null)) {
                    problems.add("Szene nicht angemeldet");
                }
                ctx.state().put(RESULT, problems);
                return StepResult.pending();
            }
            if (ctx.tick() < 40) {
                return StepResult.pending();
            }
            @SuppressWarnings("unchecked")
            List<String> problems = (List<String>) ctx.state().get(RESULT);
            Screen screen = mc.gui.screen();
            if (screen == null) {
                problems.add("kein Flugschul-Fenster offen");
            } else {
                ctx.log("Flugschul-Fenster: " + screen.getClass().getSimpleName());
            }
            mc.gui.setScreen(null);
            return StepResult.check(problems.isEmpty(), String.join("; ", problems));
        });
        serverStep("frame: Rezepte der Rahmen und Videosysteme eindeutig und freischaltbar", FrameSelfTests::checkRecipes);
        serverStep("frame: Truhen-Beute der Rahmen und Videosysteme", FrameSelfTests::checkLoot);
    }

    public static void registerPresets() {
        SelfTest.clientStep("frame: 22 Presets aller Größenklassen erzeugbar, lesbar, ohne Probleme", 60, ctx -> {
            List<String> problems = new ArrayList<>();
            Set<Identifier> registered = new LinkedHashSet<>();
            for (DronePreset preset : PropwashApi.builds().presets()) {
                registered.add(preset.id());
            }
            Set<JmpPresets.SizeClass> classes = new LinkedHashSet<>();
            for (JmpPresets.Entry entry : JmpPresets.ALL) {
                if (!registered.contains(entry.id())) {
                    problems.add(entry.path() + ": nicht angemeldet");
                    continue;
                }
                if (!has(entry.translationKey())) {
                    problems.add(entry.path() + ": Name fehlt");
                }
                Optional<BuildStats> stats = roundTrip(entry.path(), entry.data(), problems);
                stats.ifPresent(s -> {
                    classes.add(entry.sizeClass());
                    ctx.log(entry.path() + ": " + fmt(s) + " " + s.warnings());
                });
            }
            if (classes.size() != JmpPresets.SizeClass.values().length) {
                problems.add("Größenklassen ohne baubares Preset: " + classes);
            }
            return StepResult.check(problems.isEmpty(), String.join("; ", problems));
        });
    }

    private static void checkPart(String path, PartKind kind, List<String> problems) {
        Identifier id = JustMoreParts.id(path);
        Optional<PartDefinition> definition = PropwashApi.parts().get(id);
        if (definition.isEmpty()) {
            problems.add(path + ": nicht angemeldet");
            return;
        }
        if (definition.get().kind() != kind) {
            problems.add(path + ": Kategorie " + definition.get().kind());
        }
        ItemStack stack = PropwashApi.partItems().stack(id);
        if (stack.isEmpty()) {
            problems.add(path + ": kein Item");
            return;
        }
        String nameKey = "part." + JustMoreParts.MOD_ID + "." + path;
        if (!has(nameKey)) {
            problems.add(path + ": Name fehlt");
        } else if (!stack.getHoverName().getString().equals(Language.getInstance().getOrDefault(nameKey))) {
            problems.add(path + ": Item-Name " + stack.getHoverName().getString());
        }
        String help = JustMoreParts.MOD_ID + ".help." + path + ".";
        for (String line : List.of("summary", "what.1", "what.2", "use.1")) {
            if (!has(help + line)) {
                problems.add(path + ": Hilfe " + line + " fehlt");
            }
        }
    }

    private static Optional<BuildStats> roundTrip(String label, DroneData data, List<String> problems) {
        BuildCheck check = PropwashApi.builds().check(data.build());
        if (!check.known()) {
            problems.add(label + ": unbekannte Teile " + check.unknownParts());
            return Optional.empty();
        }
        if (!check.buildable()) {
            problems.add(label + ": " + check.problems());
            return Optional.empty();
        }
        try {
            ItemStack drone = PropwashApi.builds().createDrone(data);
            Optional<DroneData> read = PropwashApi.builds().read(drone);
            if (read.isEmpty() || !read.get().build().equals(data.build())) {
                problems.add(label + ": Bauplan nach dem Lesen anders");
            } else if (!read.get().paint().equals(data.paint())) {
                problems.add(label + ": Lackierung nach dem Lesen anders");
            }
        } catch (IllegalArgumentException e) {
            problems.add(label + ": " + e.getMessage());
        }
        Optional<BuildStats> stats = PropwashApi.builds().analyze(data.build());
        if (stats.isEmpty()) {
            problems.add(label + ": keine Analyse");
        }
        return stats;
    }

    private static void serverStep(String name, Function<MinecraftServer, List<String>> check) {
        SelfTest.clientStep(name, 200, ctx -> {
            if (!ctx.state().containsKey(FUTURE)) {
                AtomicReference<List<String>> result = new AtomicReference<>(List.of("keine Antwort"));
                ctx.state().put(RESULT, result);
                ctx.state().put(FUTURE, ctx.onServer(server -> result.set(check.apply(server))));
                return StepResult.pending();
            }
            CompletableFuture<?> future = (CompletableFuture<?>) ctx.state().get(FUTURE);
            if (!future.isDone()) {
                return StepResult.pending();
            }
            if (future.isCompletedExceptionally()) {
                return StepResult.fail("Server-Prüfung fehlgeschlagen: " + future.exceptionNow());
            }
            @SuppressWarnings("unchecked")
            List<String> problems = ((AtomicReference<List<String>>) ctx.state().get(RESULT)).get();
            return StepResult.check(problems.isEmpty(), String.join("; ", problems));
        });
    }

    private static List<String> checkRecipes(MinecraftServer server) {
        List<String> problems = new ArrayList<>();
        RecipeManager recipes = server.getRecipeManager();
        ServerLevel level = server.overworld();
        Map<ResourceKey<Recipe<?>>, Identifier> unlockedBy = new HashMap<>();
        for (AdvancementHolder advancement : server.getAdvancements().getAllAdvancements()) {
            for (ResourceKey<Recipe<?>> recipe : advancement.value().rewards().recipes()) {
                unlockedBy.put(recipe, advancement.id());
            }
        }
        List<String> ids = new ArrayList<>(FrameModule.FRAMES);
        ids.addAll(VideoModule.VIDEOS);
        for (String path : ids) {
            Identifier id = JustMoreParts.id(path);
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
            Optional<RecipeHolder<?>> holder = recipes.byKey(key);
            if (holder.isEmpty()) {
                problems.add("Rezept fehlt: " + path);
                continue;
            }
            if (!(holder.get().value() instanceof ShapedRecipe shaped)) {
                problems.add(path + ": kein geformtes Werkbank-Rezept");
                continue;
            }
            List<ItemStack> items = new ArrayList<>();
            ContextMap display = SlotDisplayContext.fromLevel(level);
            for (Optional<Ingredient> ingredient : shaped.getIngredients()) {
                items.add(ingredient.map(i -> i.display().resolveForFirstStack(display)).orElse(ItemStack.EMPTY));
            }
            CraftingInput input = CraftingInput.of(shaped.getWidth(), shaped.getHeight(), items);
            Optional<RecipeHolder<CraftingRecipe>> found = recipes.getRecipeFor(RecipeType.CRAFTING, input, level);
            if (found.isEmpty()) {
                problems.add(path + ": die eigenen Zutaten ergeben kein Rezept");
            } else if (!found.get().id().equals(key)) {
                problems.add(path + ": dieselben Zutaten ergeben " + found.get().id().identifier());
            }
            ItemStack result = shaped.assemble(input);
            if (!PropwashApi.partItems().partOf(result).map(id::equals).orElse(false)) {
                problems.add(path + " liefert " + result);
            }
            if (!unlockedBy.containsKey(key)) {
                problems.add(path + ": kein Freischalt-Fortschritt");
            }
        }
        return problems;
    }

    private static List<String> checkLoot(MinecraftServer server) {
        Set<String> problems = new LinkedHashSet<>();
        ServerLevel level = server.overworld();
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        Set<Identifier> allowed = new TreeSet<>();
        FrameModule.frameIds().forEach(allowed::add);
        VideoModule.videoIds().forEach(allowed::add);
        Map<ResourceKey<LootTable>, Set<Identifier>> droppable = new HashMap<>();
        for (ResourceKey<LootTable> table : FrameLoot.tables()) {
            LootTable loot = server.reloadableRegistries().getLootTable(table);
            if (loot == LootTable.EMPTY) {
                problems.add("Beutetabelle fehlt: " + table.identifier());
                continue;
            }
            Set<Identifier> seen = new TreeSet<>();
            for (int seed = 1; seed <= INJECT_ROLLS; seed++) {
                for (ItemStack stack : loot.getRandomItems(params, seed * 0x9E3779B97F4A7C15L)) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    seen.add(id);
                    if (!allowed.contains(id)) {
                        problems.add(table.identifier() + " liefert fremdes Item " + id);
                    }
                }
            }
            if (seen.isEmpty()) {
                problems.add(table.identifier() + ": in " + INJECT_ROLLS + " Würfen nichts");
            }
            droppable.put(table, seen);
        }
        boolean injected = "NeoForge".equals(Services.PLATFORM.name());
        for (FrameLoot.Injection injection : FrameLoot.injections()) {
            LootTable target = server.reloadableRegistries().getLootTable(injection.target());
            if (target == LootTable.EMPTY) {
                problems.add("Truhen-Tabelle fehlt: " + injection.target().identifier());
                continue;
            }
            if (!injected) {
                continue;
            }
            int finds = 0;
            for (int seed = 1; seed <= TARGET_ROLLS; seed++) {
                boolean found = false;
                for (ItemStack stack : target.getRandomItems(params, seed * 0x9E3779B97F4A7C15L)) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (allowed.contains(id)) {
                        found = true;
                        if (!droppable.getOrDefault(injection.table(), Set.of()).contains(id)) {
                            problems.add(injection.target().identifier() + ": " + id + " nicht aus " + injection.table().identifier());
                        }
                    }
                }
                if (found) {
                    finds++;
                }
            }
            if (finds == 0) {
                problems.add(injection.target().identifier() + ": keine Rahmen-Beute in " + TARGET_ROLLS + " Truhen");
            }
        }
        return new ArrayList<>(problems);
    }

    private static boolean has(String key) {
        return Language.getInstance().has(key);
    }

    private static String fmt(BuildStats stats) {
        if (stats == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.0f g, %.0f km/h, %.1f %% Props", stats.massGrams(), stats.topSpeedKmh(),
                stats.propsInViewPercent());
    }
}
