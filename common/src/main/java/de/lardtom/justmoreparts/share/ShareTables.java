package de.lardtom.justmoreparts.share;

import de.lardtom.propwash.api.build.TuneGroup;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.AccessorySlot;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public final class ShareTables {
    public static final String PREFIX = "PW1-";
    public static final String FORMAT = "PW1";
    public static final String REVISION = "1.2";
    public static final int FRAME_VERSION = 1;
    public static final int FLAG_DEFLATE = 1;
    public static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    public static final int GROUP_LENGTH = 5;

    public static final int MAX_INPUT_CHARS = 4000;
    public static final int MAX_FRAME_BYTES = 2500;
    public static final int MIN_FRAME_BYTES = 7;
    public static final int MAX_PAYLOAD_BYTES = 16384;
    public static final int MAX_TUNE_ENTRIES = 256;
    public static final int MAX_TUNE_KEY_CHARS = 64;
    public static final int MAX_OSD_CHARS = 8192;
    public static final int MAX_NAME_CHARS = 48;
    public static final int MAX_PATH_CHARS = 128;
    public static final int MAX_NAMESPACE_CHARS = 64;
    public static final int PAINT_SLOT_BITS = 16;

    public static final List<String> NAMESPACES = List.of("", "propwash", "justmoreparts");
    public static final List<AccessorySlot> ACCESSORY_SLOTS = accessorySlots();
    public static final List<String> PAINT_SLOTS = List.of("frame", "tpu", "motors", "prop_fl", "prop_fr", "prop_rl", "prop_rr", "stack",
            "battery", "camera", "antenna", "accessories");
    public static final List<String> TUNE_KEYS = List.of(
            "rates.type",
            "rates.roll.rcRate", "rates.roll.superRate", "rates.roll.rcExpo", "rates.roll.centerRateDps", "rates.roll.maxRateDps",
            "rates.roll.actualExpo",
            "rates.pitch.rcRate", "rates.pitch.superRate", "rates.pitch.rcExpo", "rates.pitch.centerRateDps", "rates.pitch.maxRateDps",
            "rates.pitch.actualExpo",
            "rates.yaw.rcRate", "rates.yaw.superRate", "rates.yaw.rcExpo", "rates.yaw.centerRateDps", "rates.yaw.maxRateDps",
            "rates.yaw.actualExpo",
            "rates.rcDeadband", "rates.yawDeadband", "pid.source",
            "pid.roll.p", "pid.roll.i", "pid.roll.d", "pid.roll.ff",
            "pid.pitch.p", "pid.pitch.i", "pid.pitch.d", "pid.pitch.ff",
            "pid.yaw.p", "pid.yaw.i", "pid.yaw.d", "pid.yaw.ff",
            "pid.angleStrength", "pid.angleLimitDeg", "pid.horizonStrength", "pid.itermRelax", "pid.antiGravityGain", "pid.airmode",
            "pid.tpaMode", "pid.tpaRatePercent", "pid.tpaBreakpoint", "pid.motorIdlePercent", "pid.throttleMid", "pid.throttleExpo",
            "pid.throttleLimitType", "pid.throttleLimitPercent", "pid.thrustLinearPercent", "pid.vbatSagCompensationPercent",
            "filters.pidLoop", "filters.gyroLpfHz", "filters.dtermLpf1Hz", "filters.dtermLpf2Hz", "filters.rcSmoothingHz",
            TuneSpec.CAMERA_UPTILT);
    public static final Set<TuneGroup> TUNE_GROUPS = Collections.unmodifiableSet(EnumSet.of(TuneGroup.RATES, TuneGroup.PID, TuneGroup.FEEDFORWARD,
            TuneGroup.TPA, TuneGroup.FILTERS, TuneGroup.THROTTLE, TuneGroup.CAMERA));

    public static final String UNKNOWN_NAMESPACE_PREFIX = "pw1.unknown.";

    private ShareTables() {
    }

    public static boolean sharedTuneKey(String key) {
        return TUNE_GROUPS.contains(TuneGroup.of(key));
    }

    public static int tuneIndex(String key) {
        return TUNE_KEYS.indexOf(key) + 1;
    }

    public static Optional<String> tuneKey(int index) {
        return index >= 1 && index <= TUNE_KEYS.size() ? Optional.of(TUNE_KEYS.get(index - 1)) : Optional.empty();
    }

    public static int paintBit(String slot) {
        return PAINT_SLOTS.indexOf(slot);
    }

    private static List<AccessorySlot> accessorySlots() {
        List<AccessorySlot> slots = new ArrayList<>(List.of(AccessorySlot.TOP, AccessorySlot.BOTTOM));
        for (AccessorySlot slot : AccessorySlot.values()) {
            if ("front".equals(slot.key())) {
                slots.add(slot);
            }
        }
        return List.copyOf(slots);
    }

    public static int accessoryIndex(AccessorySlot slot) {
        return ACCESSORY_SLOTS.indexOf(slot);
    }

    public static Identifier unknownNamespace(long ns, String path) {
        return Identifier.fromNamespaceAndPath(UNKNOWN_NAMESPACE_PREFIX + ns, path);
    }

    public static OptionalLong unknownNamespaceIndex(Identifier id) {
        String namespace = id.getNamespace();
        if (!namespace.startsWith(UNKNOWN_NAMESPACE_PREFIX)) {
            return OptionalLong.empty();
        }
        String digits = namespace.substring(UNKNOWN_NAMESPACE_PREFIX.length());
        if (digits.isEmpty() || digits.length() > 10 || !digits.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return OptionalLong.empty();
        }
        long value = Long.parseLong(digits);
        return value >= NAMESPACES.size() && value <= 0xFFFFFFFFL ? OptionalLong.of(value) : OptionalLong.empty();
    }

    public static String display(Identifier id) {
        if (id.getNamespace().equals(Pw2Tables.UNKNOWN_NAMESPACE)) {
            return "?" + id.getPath();
        }
        OptionalLong unknown = unknownNamespaceIndex(id);
        return unknown.isPresent() ? "?" + unknown.getAsLong() + ":" + id.getPath() : id.toString();
    }

    public static boolean validPath(String path) {
        return path.length() >= 1 && path.length() <= MAX_PATH_CHARS && onlyIdChars(path);
    }

    public static boolean validNamespace(String namespace) {
        return namespace.length() >= 1 && namespace.length() <= MAX_NAMESPACE_CHARS && onlyIdChars(namespace) && !namespace.equals("..");
    }

    private static boolean onlyIdChars(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
