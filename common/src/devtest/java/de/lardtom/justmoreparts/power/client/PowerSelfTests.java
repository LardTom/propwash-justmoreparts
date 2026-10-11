package de.lardtom.justmoreparts.power.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepContext;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.power.PowerLoot;
import de.lardtom.justmoreparts.power.PowerModule;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.part.CellChemistry;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class PowerSelfTests {
    private static final String RESULT = "result";
    private static final String FUTURE = "future";
    private static final int ROLLS = 200;

    private PowerSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep("power: alle Akkus und Stacks als Items mit Namen und Shift-Hilfe", 40, PowerSelfTests::itemsStep);
        SelfTest.clientStep("power: Zellchemie Semi-Solid-LiHV angemeldet und übersetzt", 20, PowerSelfTests::chemistryStep);
        SelfTest.clientStep("power: Aufbauten je Größenklasse bauen, erzeugen und lesen", 40, ctx -> buildsStep(ctx, builtinBuilds()));
        SelfTest.clientStep("power: 4S-Stack auf 6S meldet Überspannung (Werkbank und Analyse)", 20, PowerSelfTests::overvoltageStep);
        SelfTest.clientStep("power: Rezepte und Beutetabellen auf dem Server geladen", 200, PowerSelfTests::serverDataStep);
        SelfTest.clientStep("power: Akkus und Stacks liegen in Vanilla-Truhen", 400, PowerSelfTests::injectionStep);
        SelfTest.clientStep("power: Aufbauten mit Teilen aller Kategorien (nach der Integration)", 40, ctx -> buildsStep(ctx, integrationBuilds()));
    }

    private static Identifier id(String text) {
        return text.indexOf(':') < 0 ? JustMoreParts.id(text) : Identifier.parse(text);
    }

    private static List<PartDefinition> powerParts() {
        List<PartDefinition> out = new ArrayList<>();
        for (PartKind kind : List.of(PartKind.BATTERY, PartKind.STACK)) {
            for (PartDefinition definition : PropwashApi.parts().all(kind)) {
                if (JustMoreParts.MOD_ID.equals(definition.id().getNamespace())) {
                    out.add(definition);
                }
            }
        }
        return out;
    }

    private static StepResult itemsStep(StepContext ctx) {
        List<PartDefinition> parts = powerParts();
        List<String> errors = new ArrayList<>();
        if (parts.size() != 42) {
            errors.add(parts.size() + " statt 42 Akkus/Stacks angemeldet");
        }
        Language language = Language.getInstance();
        for (PartDefinition definition : parts) {
            String path = definition.id().getPath();
            ItemStack stack = PropwashApi.partItems().stack(definition.id());
            if (stack.isEmpty()) {
                errors.add(path + ": kein Item");
                continue;
            }
            if (!PropwashApi.partItems().partOf(stack).map(definition.id()::equals).orElse(false)) {
                errors.add(path + ": Item gehört nicht zum Teil");
            }
            for (String key : List.of("part.justmoreparts." + path, "justmoreparts.help." + path + ".summary",
                    "justmoreparts.help." + path + ".what.1", "justmoreparts.help." + path + ".what.2", "justmoreparts.help." + path + ".use.1")) {
                if (!language.has(key)) {
                    errors.add(path + ": Sprachschlüssel fehlt " + key);
                }
            }
            String name = language.getOrDefault("part.justmoreparts." + path);
            if (!stack.getHoverName().getString().contains(name)) {
                errors.add(path + ": Itemname '" + stack.getHoverName().getString() + "' statt '" + name + "'");
            }
            Rarity expected = PowerModule.rarities().getOrDefault(path, Rarity.COMMON);
            if (stack.getRarity() != expected) {
                errors.add(path + ": Seltenheit " + stack.getRarity() + " statt " + expected);
            }
            if (!definition.basics().creativeListed()) {
                errors.add(path + ": nicht im Kreativ-Tab");
            }
        }
        ctx.log(parts.size() + " Akkus/Stacks geprüft");
        return errors.isEmpty() ? StepResult.pass() : StepResult.fail(String.join("; ", errors));
    }

    private static StepResult chemistryStep(StepContext ctx) {
        Optional<CellChemistry> chemistry = PropwashApi.parts().chemistry(PowerModule.SEMI_SOLID);
        if (chemistry.isEmpty()) {
            return StepResult.fail("Zellchemie " + PowerModule.SEMI_SOLID + " fehlt");
        }
        if (Math.abs(chemistry.get().fullCellVoltage() - 4.45) > 1e-9) {
            return StepResult.fail("Ladeschluss " + chemistry.get().fullCellVoltage() + " V statt 4,45 V");
        }
        return StepResult.check(Language.getInstance().has("justmoreparts.chemistry.lihv_ss"), "Name der Zellchemie fehlt");
    }

    private record Build(String label, String[] parts, Set<String> allowedWarnings) {
        BuildSpec spec() {
            return BuildSpec.of(id(this.parts[0]), id(this.parts[1]), id(this.parts[2]), id(this.parts[3]), id(this.parts[4]), id(this.parts[5]));
        }
    }

    private static Build build(String label, String... parts) {
        return new Build(label, parts, Set.of());
    }

    private static Build build(String label, Set<String> allowed, String... parts) {
        return new Build(label, parts, allowed);
    }

    private static List<Build> builtinBuilds() {
        Set<String> lion = Set.of("battery_weak", "heavy_sag", "battery_overload");
        return List.of(
                build("Whoop 65 1S 380", "propwash:whoop_65", "aio_whoop_1s_6a", "propwash:m0802_22000", "propwash:p31_3b", "propwash:analog_nano",
                        "b1s_380_hv"),
                build("Whoop 65 1S 250", "propwash:whoop_65", "aio_whoop_1s_6a", "propwash:m0802_22000", "propwash:p31_3b", "propwash:analog_nano",
                        "b1s_250_hv"),
                build("Cinewhoop 3″ 4S 850", "propwash:cinewhoop_3", "aio_20_4s_35a", "propwash:m1404_4600", "propwash:p3_3b",
                        "propwash:digital_lite", "b4s_850"),
                build("Cinewhoop 3″ 3S 650", "propwash:cinewhoop_3", "aio_20_4s_35a", "propwash:m1404_4600", "propwash:p3_3b",
                        "propwash:digital_lite", "b3s_650"),
                build("Freestyle 5″ 6S 1300", "propwash:freestyle_5", "stack_30_6s_60a", "propwash:m2207_1750", "propwash:p51_3b",
                        "propwash:digital_hd", "propwash:b6s_1300"),
                build("Freestyle 5″ 6S 80 A", "propwash:freestyle_5", "stack_30_6s_80a", "propwash:m2207_1750", "propwash:p51_3b",
                        "propwash:digital_hd", "propwash:b6s_1300"),
                build("Long Range 7″ 6S 2200", "propwash:long_range_7", "stack_30_6s_60a", "propwash:m2807_1300", "propwash:p7_2b",
                        "propwash:digital_hd", "b6s_2200"),
                build("Long Range 7″ Li-Ion 4500", lion, "propwash:long_range_7", "stack_30_8s_65a", "propwash:m2807_1300", "propwash:p7_2b",
                        "propwash:digital_hd", "b6s_4500_lion"),
                build("Long Range 7″ Semi-Solid", lion, "propwash:long_range_7", "stack_30_6s_60a", "propwash:m2807_1300", "propwash:p7_2b",
                        "propwash:digital_hd", "b6s_3000_ss"));
    }

    private static List<Build> integrationBuilds() {
        Set<String> lion = Set.of("battery_weak", "heavy_sag", "battery_overload");
        return List.of(
                build("Whoop 75 2S 300", Set.of("heavy_sag", "battery_overload"), "whoop_75_duct", "aio_whoop_2s_12a", "m0802_15500", "p40_20_2b_pc",
                        "analog_whoop_25mw", "b2s_300_hv"),
                build("Whoop 85 2S 450", "whoop_85_duct", "aio_whoop_2s_12a", "m1102_13500", "p45_17_4b_bi", "digital_micro", "b2s_450_hv"),
                build("Toothpick 2,5″ 3S 450", "tp_25_x", "aio_tp_4s_20a", "m12025_5500", "p65_25_3b_pc", "digital_micro", "b3s_450"),
                build("Toothpick 3,5″ 6S 750", "tp_35_stretched", "aio_tp_6s_20a", "m1505_2500", "p89_30_3b_pc", "digital_micro", "b6s_750"),
                build("Cinewhoop 3,5″ 6S 850", "cw_35_duct", "stack_20_6s_40a", "m2004_1700", "p89_25_5b_pc_duct", "digital_hd_pro", "b6s_850"),
                build("Race 5″ 4S 1500", "race_5_stretched", "stack_30_4s_55a", "m23065_2450", "p130_46_3b_hard", "digital_low_latency", "b4s_1500"),
                build("Bash 5″ 6S 1500", "fs_5_bash", "stack_30_6s_60a", "m2207_1950", "p130_41_3b_soft", "digital_hd_pro", "b6s_1500"),
                build("Long Range 8″ 8S Li-Ion", lion, "lr_8_x", "stack_30_8s_65a", "m2807_1100", "p203_45_2b_cfn", "digital_lr", "b8s_4500_lion"),
                build("X-Class 10″ 6S 5000", "xclass_10", "stack_30_8s_100a", "m3115_900", "p254_50_2b_carbon", "digital_hd_pro", "b6s_5000"),
                build("X-Class 13″ 8S 5000", "xclass_13", "stack_30_8s_100a", "m4214_400", "p330_70_2b_carbon", "digital_lr", "b8s_5000"));
    }

    private static StepResult buildsStep(StepContext ctx, List<Build> builds) {
        List<String> errors = new ArrayList<>();
        for (Build build : builds) {
            BuildSpec spec = build.spec();
            BuildCheck check = PropwashApi.builds().check(spec);
            if (!check.known()) {
                errors.add(build.label() + ": Teile fehlen " + check.unknownParts());
                continue;
            }
            if (!check.buildable()) {
                errors.add(build.label() + ": nicht baubar " + check.problems());
                continue;
            }
            Set<String> warnings = new HashSet<>(check.warnings());
            Optional<BuildStats> stats = PropwashApi.builds().analyze(spec);
            if (stats.isEmpty()) {
                errors.add(build.label() + ": keine Analyse");
                continue;
            }
            warnings.addAll(stats.get().warnings());
            warnings.removeAll(build.allowedWarnings());
            if (!warnings.isEmpty()) {
                errors.add(build.label() + ": Warnungen " + warnings);
            }
            ItemStack drone = PropwashApi.builds().createDrone(DroneData.of(spec));
            if (drone.isEmpty() || !PropwashApi.builds().isDrone(drone)) {
                errors.add(build.label() + ": Drohne nicht erzeugt");
                continue;
            }
            Optional<DroneData> read = PropwashApi.builds().read(drone);
            if (read.isEmpty() || !read.get().build().equals(spec)) {
                errors.add(build.label() + ": Bauplan nicht lesbar oder verändert");
            }
            ctx.log(String.format(Locale.ROOT, "%s: %.0f g, T/W %.1f, %.1f min", build.label(), stats.get().massGrams(),
                    stats.get().thrustToWeight(), stats.get().hoverFlightTimeMin()));
        }
        return errors.isEmpty() ? StepResult.pass() : StepResult.fail(String.join("; ", errors));
    }

    private static StepResult overvoltageStep(StepContext ctx) {
        List<String> errors = new ArrayList<>();
        Map<String, String> overvolted = new LinkedHashMap<>();
        overvolted.put("stack_30_4s_55a", "propwash:b6s_1300");
        overvolted.put("stack_30_6s_60a", "b8s_2200");
        for (Map.Entry<String, String> entry : overvolted.entrySet()) {
            BuildSpec spec = BuildSpec.of(id("propwash:freestyle_5"), id(entry.getKey()), id("propwash:m2207_1750"), id("propwash:p51_3b"),
                    id("propwash:digital_hd"), id(entry.getValue()));
            BuildCheck check = PropwashApi.builds().check(spec);
            if (!check.warnings().contains("stack_voltage")) {
                errors.add(entry.getKey() + " mit " + entry.getValue() + ": keine Warnung stack_voltage " + check.warnings());
            }
            if (check.buildable() && PropwashApi.builds().analyze(spec).map(s -> !s.warnings().contains("esc_overvoltage")).orElse(true)) {
                errors.add(entry.getKey() + " mit " + entry.getValue() + ": keine Warnung esc_overvoltage");
            }
        }
        BuildSpec fine = BuildSpec.of(id("propwash:freestyle_5"), id("stack_30_4s_55a"), id("propwash:m2207_1750"), id("propwash:p51_3b"),
                id("propwash:digital_hd"), id("b4s_1500"));
        if (PropwashApi.builds().check(fine).warnings().contains("stack_voltage")) {
            errors.add("4S-Stack mit 4S-Akku meldet Überspannung");
        }
        ctx.log("Durchbrennen beim Scharfschalten meldet die Analyse (esc_overvoltage); der Flugtest braucht den API-Wunsch P1 (ESC_BURNED)");
        return errors.isEmpty() ? StepResult.pass() : StepResult.fail(String.join("; ", errors));
    }

    @SuppressWarnings("unchecked")
    private static StepResult onServer(StepContext ctx, Function<MinecraftServer, List<String>> work) {
        if (!ctx.state().containsKey(FUTURE)) {
            ctx.state().put(FUTURE, ctx.onServer(server -> ctx.state().put(RESULT, work.apply(server))));
            return StepResult.pending();
        }
        CompletableFuture<Void> future = (CompletableFuture<Void>) ctx.state().get(FUTURE);
        if (!future.isDone()) {
            return StepResult.pending();
        }
        if (future.isCompletedExceptionally()) {
            return StepResult.fail("Server-Fehler: " + future.exceptionNow());
        }
        List<String> errors = (List<String>) ctx.state().getOrDefault(RESULT, List.of("kein Ergebnis"));
        return errors.isEmpty() ? StepResult.pass() : StepResult.fail(String.join("; ", errors));
    }

    private static List<ItemStack> roll(MinecraftServer server, ResourceKey<LootTable> key, long seed) {
        LootTable table = server.reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(server.overworld()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        return new ArrayList<>(table.getRandomItems(params, seed * 0x9E3779B97F4A7C15L));
    }

    private static StepResult serverDataStep(StepContext ctx) {
        return onServer(ctx, server -> {
            List<String> errors = new ArrayList<>();
            Set<Identifier> power = new HashSet<>();
            for (PartDefinition definition : powerParts()) {
                power.add(definition.id());
                ResourceKey<Recipe<?>> recipe = ResourceKey.create(Registries.RECIPE, definition.id());
                if (server.getRecipeManager().byKey(recipe).isEmpty()) {
                    errors.add("Rezept fehlt: " + definition.id());
                }
            }
            Set<Identifier> found = new HashSet<>();
            for (ResourceKey<LootTable> key : PowerLoot.tables()) {
                if (server.reloadableRegistries().getLootTable(key) == LootTable.EMPTY) {
                    errors.add("Beutetabelle fehlt: " + key.identifier());
                    continue;
                }
                for (long seed = 1; seed <= ROLLS; seed++) {
                    for (ItemStack stack : roll(server, key, seed)) {
                        Optional<Identifier> part = PropwashApi.partItems().partOf(stack);
                        if (part.isEmpty() || !power.contains(part.get())) {
                            errors.add(key.identifier() + " würfelt fremdes Item " + stack);
                        } else {
                            found.add(part.get());
                        }
                    }
                }
            }
            ctx.log(found.size() + " von " + power.size() + " Akkus/Stacks in " + PowerLoot.tables().size() + " Tabellen gewürfelt");
            if (found.size() < power.size() / 2) {
                errors.add("nur " + found.size() + " Teile gewürfelt");
            }
            return errors;
        });
    }

    private static StepResult injectionStep(StepContext ctx) {
        return onServer(ctx, server -> {
            List<String> errors = new ArrayList<>();
            Map<ResourceKey<LootTable>, ResourceKey<LootTable>> oneTargetPerTable = new LinkedHashMap<>();
            for (PowerLoot.Injection injection : PowerLoot.injections()) {
                oneTargetPerTable.putIfAbsent(injection.table(), injection.target());
            }
            for (ResourceKey<LootTable> target : oneTargetPerTable.values()) {
                boolean hit = false;
                for (long seed = 1; seed <= ROLLS && !hit; seed++) {
                    for (ItemStack stack : roll(server, target, seed)) {
                        hit |= PropwashApi.partItems().partOf(stack).flatMap(id -> PropwashApi.parts().get(id))
                                .map(d -> JustMoreParts.MOD_ID.equals(d.id().getNamespace())
                                        && (d.kind() == PartKind.BATTERY || d.kind() == PartKind.STACK)).orElse(false);
                    }
                }
                if (!hit) {
                    errors.add(target.identifier() + ": in " + ROLLS + " Truhen kein JMP-Akku/-Stack (Einspeisung fehlt?)");
                }
            }
            ctx.log("Einspeisung geprüft: " + oneTargetPerTable.size() + " Vanilla-Truhen je " + ROLLS + " Würfe");
            return errors;
        });
    }
}
