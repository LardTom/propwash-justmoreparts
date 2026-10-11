package de.lardtom.justmoreparts.share;

import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.ToIntFunction;

public final class ShareFormat {
    private ShareFormat() {
    }

    public static Locale locale(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return Locale.ROOT;
        }
        return Locale.forLanguageTag(languageCode.replace('_', '-'));
    }

    public static String number(double value, int decimals, Locale locale) {
        if (!Double.isFinite(value)) {
            return "–";
        }
        DecimalFormat format = new DecimalFormat(decimals <= 0 ? "0" : "0." + "0".repeat(decimals), DecimalFormatSymbols.getInstance(locale));
        return format.format(value);
    }

    public static MutableComponent partName(Identifier id) {
        if (ShareTables.unknownNamespaceIndex(id).isPresent()) {
            return Component.literal(ShareTables.display(id));
        }
        return Component.translatableWithFallback("part." + id.getNamespace() + "." + id.getPath(), id.toString());
    }

    public static MutableComponent dyeName(String dye) {
        if ("custom".equals(dye)) {
            return Component.translatable("justmoreparts.share.dye.custom");
        }
        return Component.translatableWithFallback("item.minecraft." + dye + "_dye", dye);
    }

    public static MutableComponent kindName(PartKind kind) {
        return Component.translatable("justmoreparts.share.kind." + kind.key());
    }

    public static boolean namesKind(PartKind kind, Component name) {
        String kindText = kindName(kind).getString().toLowerCase(Locale.ROOT);
        return !kindText.isBlank() && name.getString().toLowerCase(Locale.ROOT).contains(kindText);
    }

    public static MutableComponent partLine(Slot slot, MutableComponent name, String formatKey) {
        if (namesKind(slot.kind(), name)) {
            return slot.count() > 1 ? Component.literal(slot.count() + "× ").append(name) : name;
        }
        MutableComponent label = slot.count() > 1 ? Component.literal(slot.count() + "× ").append(kindName(slot.kind())) : kindName(slot.kind());
        return Component.translatable(formatKey, label, name);
    }

    public static MutableComponent layerList(Collection<ShareLayer> layers) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (ShareLayer layer : ShareLayer.values()) {
            if (!layers.contains(layer)) {
                continue;
            }
            if (!first) {
                out.append(", ");
            }
            out.append(Component.translatable(layer.translationKey()));
            first = false;
        }
        return out;
    }

    public static MutableComponent idList(Collection<Identifier> ids) {
        List<String> names = new ArrayList<>();
        for (Identifier id : ids) {
            names.add(ShareTables.display(id));
        }
        return Component.literal(String.join(", ", names));
    }

    public static List<String> wrapCode(String code, int width, ToIntFunction<String> measure) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        String[] groups = code.split("-", -1);
        for (int i = 0; i < groups.length; i++) {
            String piece = i < groups.length - 1 ? groups[i] + "-" : groups[i];
            if (!row.isEmpty() && measure.applyAsInt(row + piece) > width) {
                rows.add(row.toString());
                row.setLength(0);
            }
            if (measure.applyAsInt(piece) <= width) {
                row.append(piece);
                continue;
            }
            for (int c = 0; c < piece.length(); c++) {
                if (!row.isEmpty() && measure.applyAsInt(row.toString() + piece.charAt(c)) > width) {
                    rows.add(row.toString());
                    row.setLength(0);
                }
                row.append(piece.charAt(c));
            }
        }
        if (!row.isEmpty()) {
            rows.add(row.toString());
        }
        return rows;
    }

    public static List<Slot> slots(BuildSpec build) {
        List<Slot> out = new ArrayList<>();
        out.add(new Slot(PartKind.FRAME, Optional.empty(), build.frame(), 1));
        out.add(new Slot(PartKind.STACK, Optional.empty(), build.stack(), 1));
        out.add(new Slot(PartKind.MOTOR, Optional.empty(), build.motor(), 4));
        out.add(new Slot(PartKind.PROP, Optional.empty(), build.prop(), 4));
        out.add(new Slot(PartKind.VIDEO, Optional.empty(), build.video(), 1));
        out.add(new Slot(PartKind.BATTERY, Optional.empty(), build.battery(), 1));
        for (AccessorySlot slot : ShareTables.ACCESSORY_SLOTS) {
            build.accessory(slot).ifPresent(id -> out.add(new Slot(PartKind.ACCESSORY, Optional.of(slot), id, 1)));
        }
        return out;
    }

    public record Slot(PartKind kind, Optional<AccessorySlot> accessory, Identifier id, int count) {
    }
}
