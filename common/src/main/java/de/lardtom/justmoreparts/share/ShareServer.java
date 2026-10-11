package de.lardtom.justmoreparts.share;

import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.DroneData;
import de.lardtom.propwash.api.build.ImportLayer;
import de.lardtom.propwash.api.build.ImportRequest;
import de.lardtom.propwash.api.build.ImportResult;
import de.lardtom.propwash.api.build.ImportStatus;
import de.lardtom.propwash.api.build.ImportTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Optional;

public final class ShareServer {
    private ShareServer() {
    }

    public static Optional<DroneData> held(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return PropwashApi.builds().isDrone(stack) ? PropwashApi.builds().read(stack) : Optional.empty();
    }

    public static ItemStack createDrone(ShareContent content) {
        return PropwashApi.builds().createDrone(content.toDroneData());
    }

    public static void give(ServerPlayer player, ItemStack stack) {
        ItemStack rest = stack.copy();
        boolean added = player.getInventory().add(rest);
        if (!added || !rest.isEmpty()) {
            ItemEntity drop = player.createItemStackToDrop(rest, false, false);
            if (drop != null) {
                drop.setNoPickUpDelay();
                drop.setTarget(player.getUUID());
                player.level().addFreshEntity(drop);
            }
        }
        player.containerMenu.broadcastChanges();
    }

    public static EnumSet<ImportLayer> heldLayers(ShareContent content) {
        EnumSet<ImportLayer> layers = EnumSet.noneOf(ImportLayer.class);
        if (content.paint().isPresent()) {
            layers.add(ImportLayer.PAINT);
        }
        if (content.tune().isPresent()) {
            layers.add(ImportLayer.TUNE);
        }
        return layers;
    }

    public static ImportResult applyToHeld(ServerPlayer player, ShareContent content) {
        EnumSet<ImportLayer> layers = heldLayers(content);
        if (layers.isEmpty()) {
            return ImportResult.of(ImportStatus.UNCHANGED);
        }
        return PropwashApi.builds().applyImport(player, new ImportRequest(content.toDroneData(), layers, ShareTables.TUNE_GROUPS, ImportTarget.HELD_DRONE));
    }
}
