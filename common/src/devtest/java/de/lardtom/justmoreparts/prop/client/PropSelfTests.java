package de.lardtom.justmoreparts.prop.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.prop.PropLoot;
import de.lardtom.justmoreparts.prop.PropModule;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.part.PropDefinition;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.AtlasIds;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class PropSelfTests {
    private static final int PROP_COUNT = 38;
    private static final int TABLE_ROLLS = 400;
    private static final int TARGET_ROLLS = 300;
    private static final String RESULT = "result";
    private static final String FUTURE = "future";
    private static final Identifier JMP_TAB = JustMoreParts.propwash("main-" + JustMoreParts.MOD_ID);
    private static final List<String> MODEL_VARIANTS = List.of("", "_ccw", "_blur", "_worn", "_worn_ccw", "_damaged", "_damaged_ccw",
            "_broken", "_broken_ccw");

    private PropSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep("prop: 38 Props mit Item, Namen, Hilfe und Seltenheit", 40, ctx -> itemsStep());
        SelfTest.clientStep("prop: Modelle, Texturen und Kreativ-Tab", 40, ctx -> modelsStep());
        SelfTest.clientStep("prop: Aufbau je Größenklasse erzeugen und lesen", 40, ctx -> buildsStep());
        SelfTest.clientStep("prop: Rezepte eindeutig und freigeschaltet", 200, ctx -> serverStep(ctx, PropSelfTests::checkRecipes));
        SelfTest.clientStep("prop: Beutetabellen würfeln nur ihre Props", 200, ctx -> serverStep(ctx, PropSelfTests::checkTables));
        SelfTest.clientStep("prop: Props in Vanilla-Truhen (Loader-Anbindung)", 400, ctx -> serverStep(ctx, PropSelfTests::checkChests));
    }

    private static StepResult itemsStep() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return StepResult.fail("keine Welt");
        }
        List<PropDefinition> props = PropModule.ownProps();
        List<String> problems = new ArrayList<>();
        if (props.size() != PROP_COUNT) {
            problems.add(props.size() + " JMP-Props statt " + PROP_COUNT);
        }
        Language language = Language.getInstance();
        Item.TooltipContext context = Item.TooltipContext.of(mc.level);
        for (PropDefinition prop : props) {
            String path = prop.id().getPath();
            ItemStack stack = PropwashApi.partItems().stack(prop.id());
            if (stack.isEmpty()) {
                problems.add(path + " ohne Item");
                continue;
            }
            String nameKey = "part." + JustMoreParts.MOD_ID + "." + path;
            if (!language.has(nameKey)) {
                problems.add(path + " ohne Namen");
            } else if (!stack.getHoverName().getString().equals(language.getOrDefault(nameKey))) {
                problems.add(path + " zeigt „" + stack.getHoverName().getString() + "“ statt des Namens");
            }
            String help = JustMoreParts.MOD_ID + ".help." + path;
            for (String key : List.of(help + ".summary", help + ".what.1", help + ".what.2", help + ".use.1")) {
                if (!language.has(key)) {
                    problems.add(path + ": Hilfe " + key + " fehlt");
                }
            }
            if (stack.getRarity() != PropModule.rarityOf(prop)) {
                problems.add(path + ": Seltenheit " + stack.getRarity() + " statt " + PropModule.rarityOf(prop));
            }
            if (stack.getTooltipLines(context, mc.player, TooltipFlag.NORMAL).size() < 2) {
                problems.add(path + ": Tooltip zu kurz");
            }
            if (stack.getMaxStackSize() < 16) {
                problems.add(path + ": stapelt nur bis " + stack.getMaxStackSize());
            }
        }
        return StepResult.check(problems.isEmpty(), String.join("; ", problems));
    }

    private static StepResult modelsStep() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return StepResult.fail("keine Welt");
        }
        List<String> problems = new ArrayList<>();
        TextureAtlas atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
        Optional<CreativeModeTab> tab = Optional.ofNullable(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(JMP_TAB));
        if (tab.isEmpty()) {
            problems.add("JMP-Kreativ-Tab " + JMP_TAB + " nicht gefunden");
        } else {
            CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
        }
        for (PropDefinition prop : PropModule.ownProps()) {
            String path = prop.id().getPath();
            if (mc.getModelManager().getItemModel(prop.id()) instanceof MissingItemModel) {
                problems.add(path + " ohne Item-Modell");
            }
            for (String variant : MODEL_VARIANTS) {
                if (variant.equals("_ccw")) {
                    continue;
                }
                for (String paint : List.of("", "_paint")) {
                    String texture = path + variant + paint;
                    if (atlas.getSprite(JustMoreParts.id("item/part/prop/" + texture)) == atlas.missingSprite()) {
                        problems.add("Textur " + texture + " fehlt im Item-Atlas");
                    }
                }
            }
            for (String variant : MODEL_VARIANTS) {
                for (String paint : List.of("", "_paint")) {
                    String name = path + variant + paint;
                    if (mc.getResourceManager().getResource(JustMoreParts.id("items/drone_render/prop/" + name + ".json")).isEmpty()
                            || mc.getResourceManager().getResource(JustMoreParts.id("models/part/prop/" + name + ".json")).isEmpty()) {
                        problems.add("Drohnen-Modell " + name + " fehlt");
                    }
                }
            }
            Optional<Item> item = PropwashApi.partItems().item(prop.id());
            if (tab.isPresent() && item.isPresent() && tab.get().getDisplayItems().stream().noneMatch(stack -> stack.is(item.get()))) {
                problems.add(path + " fehlt im JMP-Kreativ-Tab");
            }
        }
        return StepResult.check(problems.isEmpty(), String.join("; ", problems));
    }

    private static StepResult buildsStep() {
        Map<String, BuildSpec> builds = new LinkedHashMap<>();
        builds.put("Whoop 65 mm", spec("propwash:whoop_65", "propwash:aio_whoop", "propwash:m0802_22000", "p31_16_2b_pc",
                "propwash:analog_nano", "propwash:b1s_300_hv"));
        builds.put("Cinewhoop 3″", spec("propwash:cinewhoop_3", "propwash:aio_20", "propwash:m1404_4600", "p76_24_5b_pc_duct",
                "propwash:digital_lite", "propwash:b4s_650"));
        builds.put("Freestyle 5″", spec("propwash:freestyle_5", "propwash:stack_30", "propwash:m2207_1750", "p130_41_3b_soft",
                "propwash:digital_hd", "propwash:b6s_1300"));
        builds.put("Race 5″", spec("propwash:race_5", "propwash:stack_30", "propwash:m2306_1860", "p130_50_3b_gfn",
                "propwash:analog_race", "propwash:b6s_1100"));
        builds.put("Long Range 7″", spec("propwash:long_range_7", "propwash:stack_30", "propwash:m2807_1300", "p178_40_2b_cfn",
                "propwash:digital_hd", "propwash:b6s_4000_lion"));
        builds.put("X-Class 13″", spec("xclass_13", "stack_30_8s_100a", "m4214_400", "p330_70_2b_carbon", "digital_lr", "b8s_5000"));
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, BuildSpec> entry : builds.entrySet()) {
            BuildSpec spec = entry.getValue();
            BuildCheck check = PropwashApi.builds().check(spec);
            if (!check.buildable()) {
                problems.add(entry.getKey() + ": " + check.unknownParts() + check.problems());
                continue;
            }
            if (PropwashApi.builds().analyze(spec).isEmpty()) {
                problems.add(entry.getKey() + ": keine Analyse");
            }
            try {
                ItemStack drone = PropwashApi.builds().createDrone(DroneData.of(spec));
                Optional<DroneData> read = PropwashApi.builds().read(drone);
                if (read.isEmpty() || !read.get().build().equals(spec)) {
                    problems.add(entry.getKey() + ": gelesen " + read.map(DroneData::build).orElse(null));
                }
            } catch (IllegalArgumentException e) {
                problems.add(entry.getKey() + ": " + e.getMessage());
            }
        }
        return StepResult.check(problems.isEmpty(), String.join("; ", problems));
    }

    private static BuildSpec spec(String frame, String stack, String motor, String prop, String video, String battery) {
        return BuildSpec.of(id(frame), id(stack), id(motor), id(prop), id(video), id(battery));
    }

    private static Identifier id(String text) {
        return text.indexOf(':') < 0 ? JustMoreParts.id(text) : Identifier.parse(text);
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
        for (PropDefinition prop : PropModule.ownProps()) {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, prop.id());
            Optional<RecipeHolder<?>> holder = recipes.byKey(key);
            if (holder.isEmpty() || !(holder.get().value() instanceof ShapedRecipe shaped)) {
                problems.add(prop.id().getPath() + ": kein geformtes Werkbank-Rezept");
                continue;
            }
            List<ItemStack> items = new ArrayList<>();
            for (Optional<Ingredient> ingredient : shaped.getIngredients()) {
                items.add(ingredient.flatMap(Ingredient::getSingleItem).map(ItemStack::new).orElse(ItemStack.EMPTY));
            }
            CraftingInput input = CraftingInput.of(shaped.getWidth(), shaped.getHeight(), items);
            Optional<RecipeHolder<CraftingRecipe>> found = recipes.getRecipeFor(RecipeType.CRAFTING, input, level);
            if (found.isEmpty() || !found.get().id().equals(key)) {
                problems.add(prop.id().getPath() + ": dieselben Zutaten ergeben " + found.map(h -> h.id().identifier()).orElse(null));
            }
            ItemStack result = shaped.assemble(input);
            Optional<Identifier> part = PropwashApi.partItems().partOf(result);
            if (part.isEmpty() || !part.get().equals(prop.id())) {
                problems.add(prop.id().getPath() + " liefert " + result);
            } else if (result.getCount() != (prop.diameterMm() >= PropModule.X_CLASS_MIN_MM ? 2 : 4)) {
                problems.add(prop.id().getPath() + " liefert " + result.getCount() + " Stück");
            }
            Identifier unlock = unlockedBy.get(key);
            if (unlock == null || !JustMoreParts.MOD_ID.equals(unlock.getNamespace())) {
                problems.add(prop.id().getPath() + ": kein Freischalt-Fortschritt");
            }
        }
        return problems;
    }

    private static List<String> checkTables(MinecraftServer server) {
        List<String> problems = new ArrayList<>();
        LootParams params = new LootParams.Builder(server.overworld()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        Set<Identifier> everLooted = new TreeSet<>();
        for (ResourceKey<LootTable> table : PropLoot.tables()) {
            LootTable loot = server.reloadableRegistries().getLootTable(table);
            if (loot == LootTable.EMPTY) {
                problems.add("Beutetabelle fehlt: " + table.identifier());
                continue;
            }
            Set<Identifier> seen = new TreeSet<>();
            for (int seed = 1; seed <= TABLE_ROLLS; seed++) {
                for (ItemStack stack : loot.getRandomItems(params, seed * 0x9E3779B97F4A7C15L)) {
                    Optional<Identifier> part = PropwashApi.partItems().partOf(stack);
                    if (part.isEmpty() || !JustMoreParts.MOD_ID.equals(part.get().getNamespace())) {
                        problems.add(table.identifier() + " würfelt fremdes Item " + stack);
                    } else {
                        seen.add(part.get());
                    }
                }
            }
            if (seen.isEmpty()) {
                problems.add(table.identifier() + " würfelt nie einen Prop");
            }
            everLooted.addAll(seen);
        }
        Set<Identifier> missing = new LinkedHashSet<>();
        for (PropDefinition prop : PropModule.ownProps()) {
            if (!everLooted.contains(prop.id())) {
                missing.add(prop.id());
            }
        }
        if (!missing.isEmpty()) {
            problems.add("nie in Truhen gefunden: " + missing);
        }
        return problems;
    }

    private static List<String> checkChests(MinecraftServer server) {
        List<String> problems = new ArrayList<>();
        LootParams params = new LootParams.Builder(server.overworld()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        for (PropLoot.Injection injection : PropLoot.injections()) {
            LootTable target = server.reloadableRegistries().getLootTable(injection.target());
            if (target == LootTable.EMPTY) {
                problems.add("Truhen-Tabelle fehlt: " + injection.target().identifier());
                continue;
            }
            int finds = 0;
            for (int seed = 1; seed <= TARGET_ROLLS; seed++) {
                for (ItemStack stack : target.getRandomItems(params, seed * 0x9E3779B97F4A7C15L)) {
                    Optional<Identifier> part = PropwashApi.partItems().partOf(stack);
                    if (part.isPresent() && JustMoreParts.MOD_ID.equals(part.get().getNamespace()) && part.get().getPath().startsWith("p")) {
                        finds++;
                    }
                }
            }
            if (finds == 0) {
                problems.add(injection.target().identifier() + ": keine JMP-Props in " + TARGET_ROLLS + " Truhen");
            }
        }
        return problems;
    }

    @FunctionalInterface
    private interface ServerCheck {
        List<String> run(MinecraftServer server);
    }

    @SuppressWarnings("unchecked")
    private static StepResult serverStep(SelfTest.StepContext ctx, ServerCheck check) {
        if (ctx.tick() == 0) {
            AtomicReference<List<String>> result = new AtomicReference<>();
            ctx.state().put(RESULT, result);
            ctx.state().put(FUTURE, ctx.onServer(server -> result.set(check.run(server))));
            return StepResult.pending();
        }
        CompletableFuture<?> future = (CompletableFuture<?>) ctx.state().get(FUTURE);
        if (!future.isDone()) {
            return StepResult.pending();
        }
        if (future.isCompletedExceptionally()) {
            return StepResult.fail("Server-Prüfung fehlgeschlagen: " + future.exceptionNow());
        }
        List<String> problems = ((AtomicReference<List<String>>) ctx.state().get(RESULT)).get();
        return StepResult.check(problems.isEmpty(), String.join("; ", problems));
    }
}
