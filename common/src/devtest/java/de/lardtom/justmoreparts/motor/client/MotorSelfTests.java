package de.lardtom.justmoreparts.motor.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.motor.MotorLoot;
import de.lardtom.justmoreparts.motor.MotorModule;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildCheck;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.BuildStats;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.part.MotorDefinition;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class MotorSelfTests {
    public static final int MOTOR_COUNT = 34;
    private static final int LOOT_ROLLS = 300;

    private MotorSelfTests() {
    }

    public static void register() {
        SelfTest.clientStep("motor: 34 Motor-Items mit Namen, Shift-Hilfe und Seltenheit", 40, ctx -> {
            List<String> errors = new ArrayList<>();
            List<MotorDefinition> motors = motors();
            if (motors.size() != MOTOR_COUNT) {
                errors.add(motors.size() + " JMP-Motoren statt " + MOTOR_COUNT);
            }
            Language language = Language.getInstance();
            for (String mount : new String[]{"m31", "m42"}) {
                if (!language.has("justmoreparts.mount.motor." + mount)) {
                    errors.add("Halterung ohne Namen: " + mount);
                }
            }
            Minecraft mc = Minecraft.getInstance();
            for (MotorDefinition motor : motors) {
                Identifier id = motor.id();
                String path = id.getPath();
                Optional<Item> item = PropwashApi.partItems().item(id);
                if (item.isEmpty()) {
                    errors.add(path + ": kein Item");
                    continue;
                }
                for (String key : new String[]{"part.justmoreparts." + path, "justmoreparts.help." + path + ".summary",
                        "justmoreparts.help." + path + ".what.1", "justmoreparts.help." + path + ".what.2", "justmoreparts.help." + path + ".use.1"}) {
                    if (!language.has(key)) {
                        errors.add(path + ": Text fehlt " + key);
                    }
                }
                ItemStack stack = PropwashApi.partItems().stack(id);
                String name = stack.getHoverName().getString();
                if (!name.equals(language.getOrDefault("part.justmoreparts." + path)) || !name.contains(String.valueOf(motor.kv()))) {
                    errors.add(path + ": Name „" + name + "“");
                }
                if (stack.getRarity() != MotorModule.rarity(motor)) {
                    errors.add(path + ": Seltenheit " + stack.getRarity());
                }
                List<Component> tooltip = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
                if (tooltip.stream().noneMatch(line -> line.getString().contains(motor.kv() + " KV"))) {
                    errors.add(path + ": Tooltip ohne KV-Zeile");
                }
            }
            ctx.log(motors.size() + " Motoren, " + motors.stream().filter(m -> MotorModule.rarity(m) != Rarity.COMMON).count()
                    + " mit Seltenheit");
            return errors.isEmpty() ? SelfTest.StepResult.pass() : SelfTest.StepResult.fail(String.join("; ", errors));
        });

        SelfTest.clientStep("motor: je Größenklasse ein Aufbau baubar, als Drohne erzeugt und wieder gelesen", 40, ctx -> {
            List<String> errors = new ArrayList<>();
            for (Map.Entry<String, BuildSpec> entry : classBuilds().entrySet()) {
                String label = entry.getKey();
                BuildSpec spec = entry.getValue();
                BuildCheck check = PropwashApi.builds().check(spec);
                if (!check.known()) {
                    errors.add(label + ": unbekannte Teile " + check.unknownParts());
                    continue;
                }
                if (!check.buildable() || !check.problems().isEmpty()) {
                    errors.add(label + ": nicht baubar " + check.problems());
                    continue;
                }
                Optional<BuildStats> stats = PropwashApi.builds().analyze(spec);
                if (stats.isEmpty()) {
                    errors.add(label + ": keine Analyse");
                } else if (stats.get().warnings().contains("sluggish_motors") || stats.get().warnings().contains("motor_thermal")) {
                    errors.add(label + ": " + stats.get().warnings());
                }
                ItemStack drone = PropwashApi.builds().createDrone(DroneData.of(spec));
                Optional<DroneData> read = PropwashApi.builds().read(drone);
                if (!PropwashApi.builds().isDrone(drone) || read.isEmpty() || !read.get().build().equals(spec)) {
                    errors.add(label + ": Drohnen-Item liest den Bauplan nicht zurück");
                }
            }
            return errors.isEmpty() ? SelfTest.StepResult.pass() : SelfTest.StepResult.fail(String.join("; ", errors));
        });

        SelfTest.clientStep("motor: 4S-Motor auf 6S meldet Überspannung", 20, ctx -> {
            BuildSpec sixCells = BuildSpec.of(pw("freestyle_5"), pw("stack_30"), JustMoreParts.id("m2207_2550"), pw("p51_3b"), pw("digital_hd"),
                    pw("b6s_1300"));
            BuildSpec fourCells = BuildSpec.of(pw("freestyle_5"), pw("stack_30"), JustMoreParts.id("m2207_2550"), pw("p51_3b"), pw("digital_hd"),
                    pw("b4s_650"));
            List<String> six = PropwashApi.builds().check(sixCells).warnings();
            List<String> four = PropwashApi.builds().check(fourCells).warnings();
            return SelfTest.StepResult.check(six.contains("motor_overvoltage") && !four.contains("motor_overvoltage"),
                    "Warnungen 6S " + six + ", 4S " + four);
        });

        SelfTest.clientStep("motor: Rezepte, Beutetabellen und Truhen-Einspeisung auf dem Server", 400, ctx -> {
            Map<String, Object> state = ctx.state();
            if (!state.containsKey("future")) {
                List<String> errors = Collections.synchronizedList(new ArrayList<>());
                List<String> info = Collections.synchronizedList(new ArrayList<>());
                state.put("errors", errors);
                state.put("info", info);
                state.put("future", ctx.onServer(server -> serverChecks(server, errors, info)));
                return SelfTest.StepResult.pending();
            }
            CompletableFuture<?> future = (CompletableFuture<?>) state.get("future");
            if (!future.isDone()) {
                return SelfTest.StepResult.pending();
            }
            if (future.isCompletedExceptionally()) {
                return SelfTest.StepResult.fail("Server-Prüfung mit Ausnahme abgebrochen");
            }
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) state.get("errors");
            @SuppressWarnings("unchecked")
            List<String> info = (List<String>) state.get("info");
            info.forEach(ctx::log);
            return errors.isEmpty() ? SelfTest.StepResult.pass() : SelfTest.StepResult.fail(String.join("; ", errors));
        });
    }

    private static void serverChecks(MinecraftServer server, List<String> errors, List<String> info) {
        int recipes = 0;
        for (MotorDefinition motor : motors()) {
            if (server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, motor.id())).isEmpty()) {
                errors.add("Rezept fehlt: " + motor.id());
            } else {
                recipes++;
            }
        }
        ServerLevel level = server.overworld();
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        int ownDrops = 0;
        for (ResourceKey<LootTable> key : MotorLoot.tables()) {
            LootTable table = server.reloadableRegistries().getLootTable(key);
            if (table == LootTable.EMPTY) {
                errors.add("Beutetabelle fehlt: " + key.identifier());
                continue;
            }
            int motors = 0;
            for (int seed = 1; seed <= LOOT_ROLLS; seed++) {
                for (ItemStack stack : table.getRandomItems(params, seed * 0x9E3779B97F4A7C15L)) {
                    if (isMotor(stack)) {
                        motors++;
                    } else if (!stack.isEmpty()) {
                        errors.add(key.identifier() + " liefert " + stack.getItem());
                    }
                }
            }
            if (motors == 0) {
                errors.add(key.identifier() + " liefert keine Motoren");
            }
            ownDrops += motors;
        }
        int injected = 0;
        List<String> missing = new ArrayList<>();
        for (MotorLoot.Injection injection : MotorLoot.injections()) {
            LootTable table = server.reloadableRegistries().getLootTable(injection.target());
            int motors = 0;
            for (int seed = 1; seed <= LOOT_ROLLS && motors == 0; seed++) {
                for (ItemStack stack : table.getRandomItems(params, seed * 0x9E3779B97F4A7C15L)) {
                    if (isMotor(stack)) {
                        motors++;
                    }
                }
            }
            if (motors == 0) {
                missing.add(injection.target().identifier().getPath());
            } else {
                injected++;
            }
        }
        if (!missing.isEmpty()) {
            errors.add("keine JMP-Motoren in " + missing.size() + " Truhen (" + String.join(", ", missing)
                    + "); Fabric braucht die Einspeisung aus MotorLoot (Architektur-Wunsch)");
        }
        info.add(recipes + " Rezepte, " + ownDrops + " Motoren aus eigenen Tabellen, " + injected + "/" + MotorLoot.injections().size()
                + " Truhen mit Motoren");
    }

    private static boolean isMotor(ItemStack stack) {
        Optional<Identifier> part = PropwashApi.partItems().partOf(stack);
        return part.isPresent() && JustMoreParts.MOD_ID.equals(part.get().getNamespace())
                && PropwashApi.parts().get(MotorDefinition.class, part.get()).isPresent();
    }

    private static List<MotorDefinition> motors() {
        List<MotorDefinition> out = new ArrayList<>();
        for (PartDefinition definition : PropwashApi.parts().all(PartKind.MOTOR)) {
            if (definition instanceof MotorDefinition motor && JustMoreParts.MOD_ID.equals(motor.id().getNamespace())) {
                out.add(motor);
            }
        }
        return out;
    }

    private static Map<String, BuildSpec> classBuilds() {
        Map<String, BuildSpec> builds = new LinkedHashMap<>();
        builds.put("Whoop 65 mm 1S", BuildSpec.of(pw("whoop_65"), pw("aio_whoop"), JustMoreParts.id("m0702_23000"), pw("p31_3b"),
                pw("analog_nano"), pw("b1s_300_hv")));
        builds.put("Cinewhoop 3\" 4S", BuildSpec.of(pw("cinewhoop_3"), pw("aio_20"), JustMoreParts.id("m1404_3800"), pw("p3_3b"),
                pw("digital_lite"), pw("b4s_650")));
        builds.put("Freestyle 5\" 6S", BuildSpec.of(pw("freestyle_5"), pw("stack_30"), JustMoreParts.id("m2207_1950"), pw("p51_3b"),
                pw("digital_hd"), pw("b6s_1300")));
        builds.put("Race 5\" 6S", BuildSpec.of(pw("race_5"), pw("stack_30"), JustMoreParts.id("m23065_2020"), pw("p51_3b"),
                pw("analog_race"), pw("b6s_1300")));
        builds.put("Long Range 7\" 6S", BuildSpec.of(pw("long_range_7"), pw("stack_30"), JustMoreParts.id("m28065_1300"), pw("p7_2b"),
                pw("digital_hd"), pw("b6s_4000_lion")));
        builds.put("X-Class 13\" 8S", BuildSpec.of(JustMoreParts.id("xclass_13"), JustMoreParts.id("stack_30_8s_100a"),
                JustMoreParts.id("m4214_400"), JustMoreParts.id("p330_70_2b_carbon"), JustMoreParts.id("digital_lr"), JustMoreParts.id("b8s_5000")));
        return builds;
    }

    private static Identifier pw(String path) {
        return JustMoreParts.propwash(path);
    }
}
