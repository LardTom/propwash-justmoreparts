package de.lardtom.justmoreparts.dev.client;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.dev.SelfTest.StepResult;
import de.lardtom.justmoreparts.preset.JmpPresets;
import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.client.PropwashClientApi;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class GalleryShots {
    public static final List<String> HOTBAR_PRESETS = List.of("whoop75_1s", "toothpick35_6s", "deadcat5_6s", "lr7_deadcat_lion", "xclass13_8s");
    public static final List<String> HOTBAR_PARTS = List.of("b6s_2200", "stack_30_8s_100a", "digital_hd_pro");
    public static final int EMPTY_HOTBAR_SLOT = 8;
    public static final List<String> FRAMES = List.of("whoop_75_duct", "tp_35_stretched", "cw_35_duct", "deadcat_5", "fs_5_squashed",
            "hframe_5", "lr_7_deadcat", "xclass_10", "xclass_13_h");
    public static final List<String> MOTORS = List.of("m0702_27000", "m1102_13500", "m1404_3800", "m1505_2500", "m2004_1700", "m2207_1950",
            "m2807_1100", "m3115_900", "m4214_400");
    public static final List<String> PROPS = List.of("p31_14_3b_bi", "p40_14_5b_soft", "p65_25_3b_pc", "p89_25_5b_pc_duct", "p130_41_3b_soft",
            "p130_50_3b_gfn", "p178_40_2b_cfn", "p254_50_2b_carbon", "p330_60_3b_cfn");
    public static final String TOOLTIP_PART = "p130_41_3b_soft";
    public static final int EMPTY_CRAFTING_SLOT = 1;
    private static final String FUTURE = "future";
    private static final String SINCE = "since";
    private static final int ARENA_OFFSET = -24;
    private static final float WORLD_YAW = 180.0F;
    private static final float WORLD_PITCH = 38.0F;
    private static final float DRONE_YAW = 30.0F;
    private static volatile BlockPos arena;

    private GalleryShots() {
    }

    public static void register() {
        Screenshots.step("bildschirmfotos: JMP-Teile im Inventar", "jmp_inventory", 30, new InventoryShot(false));
        Screenshots.step("bildschirmfotos: Shift-Hilfe eines JMP-Props", "jmp_tooltip_shift", 30, new InventoryShot(true));
        Screenshots.step("bildschirmfotos: X-Class 13″ neben einem Whoop 75 in der Welt", "jmp_xclass_whoop", 40, new WorldShot());
    }

    static int menuSlotOf(String path) {
        int index = PROPS.indexOf(path);
        return index >= 0 ? 27 + index : EMPTY_CRAFTING_SLOT;
    }

    private static void fill(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        int slot = 0;
        for (String preset : HOTBAR_PRESETS) {
            inventory.setItem(slot++, PropwashApi.builds().createDrone(JmpPresets.byPath(preset).data()));
        }
        for (String part : HOTBAR_PARTS) {
            inventory.setItem(slot++, PropwashApi.partItems().stack(JustMoreParts.id(part)));
        }
        slot = 9;
        for (List<String> row : List.of(FRAMES, MOTORS, PROPS)) {
            for (String part : row) {
                inventory.setItem(slot++, PropwashApi.partItems().stack(JustMoreParts.id(part)));
            }
        }
        inventory.setSelectedSlot(EMPTY_HOTBAR_SLOT);
        player.inventoryMenu.sendAllDataToRemote();
    }

    private static void daylight(MinecraftServer server) {
        for (String command : List.of("time set noon", "weather clear")) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            throw new IllegalStateException("kein Spieler auf dem Server");
        }
        return players.getFirst();
    }

    private static final class GalleryInventoryScreen extends InventoryScreen {
        private final int hoveredMenuSlot;

        private GalleryInventoryScreen(Player player, int hoveredMenuSlot) {
            super(player);
            this.hoveredMenuSlot = hoveredMenuSlot;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Slot slot = this.getMenu().getSlot(this.hoveredMenuSlot);
            super.extractRenderState(graphics, this.leftPos + slot.x + 8, this.topPos + slot.y + 8, a);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static final class InventoryShot implements Screenshots.Scene {
        private final boolean tooltip;

        private InventoryShot(boolean tooltip) {
            this.tooltip = tooltip;
        }

        @Override
        public StepResult prepare(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            Map<String, Object> state = ctx.state();
            if (!state.containsKey(FUTURE)) {
                mc.gui.setScreen(null);
                state.put(FUTURE, ctx.onServer(server -> {
                    ServerPlayer player = player(server);
                    player.closeContainer();
                    player.setGameMode(GameType.SURVIVAL);
                    daylight(server);
                    fill(player);
                }));
                return StepResult.pending();
            }
            CompletableFuture<?> future = (CompletableFuture<?>) state.get(FUTURE);
            if (!future.isDone()) {
                return StepResult.pending();
            }
            if (future.isCompletedExceptionally()) {
                return StepResult.fail("Inventar füllen: " + future.exceptionNow());
            }
            LocalPlayer player = mc.player;
            if (player == null || player.hasInfiniteMaterials() || player.getInventory().getItem(35).isEmpty()) {
                return ctx.tick() > 300 ? StepResult.fail("JMP-Teile kommen nicht im Inventar an") : StepResult.pending();
            }
            if (player.getInventory().getSelectedSlot() != EMPTY_HOTBAR_SLOT) {
                player.getInventory().setSelectedSlot(EMPTY_HOTBAR_SLOT);
                return StepResult.pending();
            }
            PropwashClientApi.forceShiftHelp(this.tooltip ? Boolean.TRUE : null);
            if (!(mc.gui.screen() instanceof GalleryInventoryScreen)) {
                mc.gui.setScreen(new GalleryInventoryScreen(player, this.tooltip ? menuSlotOf(TOOLTIP_PART) : EMPTY_CRAFTING_SLOT));
                state.put(SINCE, ctx.tick());
                return StepResult.pending();
            }
            return ctx.tick() - (int) state.get(SINCE) > 5 ? StepResult.pass() : StepResult.pending();
        }

        @Override
        public void finish(SelfTest.StepContext ctx) {
            PropwashClientApi.forceShiftHelp(null);
            Minecraft.getInstance().gui.setScreen(null);
            ctx.onServer(server -> {
                ServerPlayer player = player(server);
                player.getInventory().clearContent();
                player.setGameMode(GameType.CREATIVE);
                player.inventoryMenu.sendAllDataToRemote();
            });
        }
    }

    private static final class WorldShot implements Screenshots.Scene {
        @Override
        public StepResult prepare(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            Map<String, Object> state = ctx.state();
            if (!state.containsKey(FUTURE)) {
                mc.gui.setScreen(null);
                state.put(FUTURE, ctx.onServer(GalleryShots::arrange));
                return StepResult.pending();
            }
            CompletableFuture<?> future = (CompletableFuture<?>) state.get(FUTURE);
            if (!future.isDone()) {
                return StepResult.pending();
            }
            if (future.isCompletedExceptionally()) {
                return StepResult.fail("Drohnen aufstellen: " + future.exceptionNow());
            }
            BlockPos o = arena;
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null || o == null) {
                return StepResult.fail("kein Spieler");
            }
            long drones = mc.level.getEntities((Entity) null, area(o), entity -> !(entity instanceof Player)).size();
            if (drones < 2 || player.position().distanceTo(viewPoint(o)) > 0.3) {
                return ctx.tick() > 400 ? StepResult.fail("Aufbau unvollständig: " + drones + " Drohnen") : StepResult.pending();
            }
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            this.hold(ctx);
            if (!state.containsKey(SINCE)) {
                state.put(SINCE, ctx.tick());
            }
            return ctx.tick() - (int) state.get(SINCE) > 20 ? StepResult.pass() : StepResult.pending();
        }

        @Override
        public void hold(SelfTest.StepContext ctx) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            player.setYRot(WORLD_YAW);
            player.setXRot(WORLD_PITCH);
            player.yRotO = WORLD_YAW;
            player.xRotO = WORLD_PITCH;
            player.setYHeadRot(WORLD_YAW);
            player.yHeadRotO = WORLD_YAW;
            player.setYBodyRot(WORLD_YAW);
            player.yBodyRotO = WORLD_YAW;
            player.setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void finish(SelfTest.StepContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
            ctx.onServer(server -> {
                ServerPlayer player = player(server);
                BlockPos o = arena;
                if (o != null) {
                    for (Entity entity : player.level().getEntities((Entity) null, area(o), e -> !(e instanceof Player))) {
                        entity.discard();
                    }
                }
                player.getInventory().clearContent();
                player.inventoryMenu.sendAllDataToRemote();
            });
        }
    }

    private static AABB area(BlockPos o) {
        return new AABB(o).inflate(6.0, 3.0, 6.0);
    }

    private static Vec3 viewPoint(BlockPos o) {
        return new Vec3(o.getX() + 0.8, o.getY(), o.getZ() + 2.2);
    }

    private static void arrange(MinecraftServer server) {
        ServerPlayer player = player(server);
        ServerLevel level = player.level();
        daylight(server);
        if (arena == null) {
            BlockPos at = player.blockPosition();
            int x = at.getX() + ARENA_OFFSET;
            int z = at.getZ() + ARENA_OFFSET;
            arena = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        }
        BlockPos o = arena;
        for (Entity entity : level.getEntities((Entity) null, area(o), e -> !(e instanceof Player))) {
            entity.discard();
        }
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                level.setBlockAndUpdate(o.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int dy = 0; dy <= 3; dy++) {
                    level.setBlockAndUpdate(o.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        player.setGameMode(GameType.CREATIVE);
        player.teleportTo(level, o.getX() + 0.5, o.getY(), o.getZ() - 3.0, Set.of(), DRONE_YAW, 0.0F, true);
        place(player, level, o.getX() + 0.15, o.getZ() + 0.3, "xclass13_8s");
        place(player, level, o.getX() + 1.5, o.getZ() + 0.9, "whoop75_1s");
        Vec3 view = viewPoint(o);
        player.teleportTo(level, view.x, view.y, view.z, Set.of(), WORLD_YAW, WORLD_PITCH, true);
        player.getInventory().clearContent();
        player.inventoryMenu.sendAllDataToRemote();
    }

    private static void place(ServerPlayer player, ServerLevel level, double x, double z, String preset) {
        ItemStack drone = PropwashApi.builds().createDrone(JmpPresets.byPath(preset).data());
        player.setItemInHand(InteractionHand.MAIN_HAND, drone);
        BlockPos ground = BlockPos.containing(x, arena.getY() - 1, z);
        Vec3 click = new Vec3(x, ground.getY() + 1.0, z);
        drone.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(click, Direction.UP, ground, false)));
    }
}
