package de.lardtom.justmoreparts.share;

import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.AccessorySlot;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

final class ShareParsed {
    final List<ShareLayer> layers = new ArrayList<>();
    final List<String> skipped = new ArrayList<>();
    final LinkedHashMap<Identifier, PartAliases.Rename> renamed = new LinkedHashMap<>();
    final EnumMap<AccessorySlot, Identifier> accessories = new EnumMap<>(AccessorySlot.class);
    Identifier[] parts;
    PaintSpec paint;
    TuneSpec tune;
    ShareOsd osd;
    String name;

    void skipOnce(String marker) {
        if (!this.skipped.contains(marker)) {
            this.skipped.add(marker);
        }
    }
}
