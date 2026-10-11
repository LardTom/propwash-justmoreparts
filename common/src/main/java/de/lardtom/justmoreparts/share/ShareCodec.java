package de.lardtom.justmoreparts.share;

import de.lardtom.propwash.api.PropwashApi;
import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.PartDefinition;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class ShareCodec {
    private static final int HARD_INPUT_LIMIT = 65536;

    private ShareCodec() {
    }

    @FunctionalInterface
    public interface PartLookup {
        PartLookup PROPWASH = id -> PropwashApi.parts().get(id).map(PartDefinition::kind);
        PartLookup NONE = id -> Optional.empty();

        Optional<PartKind> kindOf(Identifier id);
    }

    public static String encode(ShareContent content) {
        Objects.requireNonNull(content, "content");
        return Pw2Codec.encode(content);
    }

    public static String encodePw1(ShareContent content) {
        return encodePw1(content, ShareCompression.AUTO);
    }

    public static String encodePw1(ShareContent content, ShareCompression compression) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(compression, "compression");
        byte[] payload = payload(content);
        byte[] stored = payload;
        int flags = 0;
        if (compression != ShareCompression.NEVER) {
            byte[] packed = deflate(payload);
            if (compression == ShareCompression.ALWAYS || packed.length < payload.length) {
                stored = packed;
                flags = ShareTables.FLAG_DEFLATE;
            }
        }
        ByteWriter frame = new ByteWriter().u8(ShareTables.FRAME_VERSION).u8(flags).bytes(stored);
        byte[] head = frame.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(head);
        frame.u32(crc.getValue());
        return ShareTables.PREFIX + Base32.group(Base32.encode(frame.toByteArray()));
    }

    public static boolean fits(String code) {
        return code != null && code.length() <= ShareTables.MAX_INPUT_CHARS;
    }

    static byte[] payload(ShareContent content) {
        int mask = ShareLayer.PARTS.mask();
        List<byte[]> blocks = new ArrayList<>();
        blocks.add(partsBlock(content.build()));
        if (content.paint().isPresent()) {
            mask |= ShareLayer.PAINT.mask();
            blocks.add(paintBlock(content.paint().get()));
        }
        if (content.tune().isPresent()) {
            mask |= ShareLayer.TUNE.mask();
            blocks.add(tuneBlock(content.tune().get()));
        }
        if (content.osd().isPresent()) {
            mask |= ShareLayer.OSD.mask();
            ShareOsd osd = content.osd().get();
            blocks.add(new ByteWriter().varint(osd.version()).str(osd.json()).toByteArray());
        }
        if (content.name().isPresent()) {
            mask |= ShareLayer.NAME.mask();
            blocks.add(new ByteWriter().str(content.name().get()).toByteArray());
        }
        ByteWriter out = new ByteWriter().varint(mask);
        for (byte[] block : blocks) {
            out.block(block);
        }
        return out.toByteArray();
    }

    private static byte[] partsBlock(BuildSpec build) {
        ByteWriter out = new ByteWriter();
        for (Identifier id : List.of(build.frame(), build.stack(), build.motor(), build.prop(), build.video(), build.battery())) {
            writeRef(out, id);
        }
        List<AccessorySlot> used = new ArrayList<>();
        for (AccessorySlot slot : ShareTables.ACCESSORY_SLOTS) {
            if (build.accessory(slot).isPresent()) {
                used.add(slot);
            }
        }
        for (AccessorySlot slot : build.accessories().keySet()) {
            if (!ShareTables.ACCESSORY_SLOTS.contains(slot)) {
                throw new ShareException("justmoreparts.share.error.accessory_slot", slot.key());
            }
        }
        out.u8(used.size());
        for (AccessorySlot slot : used) {
            out.u8(ShareTables.accessoryIndex(slot));
            writeRef(out, build.accessory(slot).orElseThrow());
        }
        return out.toByteArray();
    }

    private static void writeRef(ByteWriter out, Identifier id) {
        String path = id.getPath();
        if (!ShareTables.validPath(path)) {
            throw new ShareException("justmoreparts.share.error.part_id", id.toString());
        }
        OptionalLong unknown = ShareTables.unknownNamespaceIndex(id);
        if (unknown.isPresent()) {
            out.varint(unknown.getAsLong()).str(path);
            return;
        }
        String namespace = id.getNamespace();
        int index = ShareTables.NAMESPACES.indexOf(namespace);
        if (index > 0) {
            out.varint(index).str(path);
            return;
        }
        if (!ShareTables.validNamespace(namespace)) {
            throw new ShareException("justmoreparts.share.error.namespace", id.toString());
        }
        out.varint(0).str(namespace).str(path);
    }

    private static byte[] paintBlock(PaintSpec paint) {
        int bits = 0;
        ByteWriter colors = new ByteWriter();
        for (int i = 0; i < ShareTables.PAINT_SLOTS.size(); i++) {
            Optional<Integer> rgb = paint.color(ShareTables.PAINT_SLOTS.get(i));
            if (rgb.isPresent()) {
                bits |= 1 << i;
                int value = rgb.get();
                colors.u8(value >>> 16).u8(value >>> 8).u8(value);
            }
        }
        for (String slot : paint.colors().keySet()) {
            if (ShareTables.paintBit(slot) < 0) {
                throw new ShareException("justmoreparts.share.error.paint_slot", slot);
            }
        }
        return new ByteWriter().u16(bits).bytes(colors.toByteArray()).toByteArray();
    }

    private static byte[] tuneBlock(TuneSpec tune) {
        List<Map.Entry<String, Float>> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : tune.values().entrySet()) {
            if (ShareTables.sharedTuneKey(entry.getKey())) {
                entries.add(entry);
            }
        }
        entries.sort(Comparator.<Map.Entry<String, Float>>comparingInt(e -> {
            int index = ShareTables.tuneIndex(e.getKey());
            return index == 0 ? Integer.MAX_VALUE : index;
        }).thenComparing(Map.Entry::getKey));
        ByteWriter out = new ByteWriter().varint(entries.size());
        for (Map.Entry<String, Float> entry : entries) {
            int index = ShareTables.tuneIndex(entry.getKey());
            float value = entry.getValue();
            long q = scaled(value);
            boolean raw = q == Long.MIN_VALUE;
            out.varint(((long) index << 1) | (raw ? 1 : 0));
            if (index == 0) {
                out.str(entry.getKey());
            }
            if (raw) {
                out.f32(value);
            } else {
                out.zigzag(q);
            }
        }
        return out.toByteArray();
    }

    static long scaled(float value) {
        if (!Float.isFinite(value)) {
            return Long.MIN_VALUE;
        }
        double product = (double) value * 1000.0;
        if (Math.abs(product) >= 2147483647.5) {
            return Long.MIN_VALUE;
        }
        long q = Math.round(product);
        if (Math.abs(q) >= (1L << 31)) {
            return Long.MIN_VALUE;
        }
        float back = (float) (q / 1000.0);
        return Float.floatToRawIntBits(back) == Float.floatToRawIntBits(value) ? q : Long.MIN_VALUE;
    }

    private static byte[] deflate(byte[] payload) {
        return deflate(payload, null);
    }

    static byte[] deflate(byte[] payload, byte[] dictionary) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        try {
            deflater.setStrategy(Deflater.DEFAULT_STRATEGY);
            if (dictionary != null) {
                deflater.setDictionary(dictionary);
            }
            deflater.setInput(payload);
            deflater.finish();
            ByteWriter out = new ByteWriter();
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int n = deflater.deflate(buffer);
                out.bytes(Arrays.copyOf(buffer, n));
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    public static ShareDecoded decode(String code) {
        return decode(code, PartLookup.PROPWASH, PartAliases.all());
    }

    public static ShareDecoded decode(String code, PartLookup lookup) {
        return decode(code, lookup, PartAliases.all());
    }

    static ShareDecoded decode(String code, PartLookup lookup, Map<Identifier, Identifier> aliases) {
        try {
            return decodeChecked(code, lookup, aliases);
        } catch (ShareFormatException e) {
            return ShareDecoded.failed(e.status());
        } catch (RuntimeException e) {
            return ShareDecoded.failed(ShareStatus.CORRUPT);
        }
    }

    private static ShareDecoded decodeChecked(String code, PartLookup lookup, Map<Identifier, Identifier> aliases) throws ShareFormatException {
        if (code == null) {
            return ShareDecoded.failed(ShareStatus.EMPTY);
        }
        if (code.length() > HARD_INPUT_LIMIT) {
            return ShareDecoded.failed(ShareStatus.TOO_LONG);
        }
        StringBuilder text = new StringBuilder(code.length());
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) {
                text.append(c);
            }
        }
        String compact = text.toString();
        String s = compact.toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            return ShareDecoded.failed(ShareStatus.EMPTY);
        }
        if (s.length() > ShareTables.MAX_INPUT_CHARS) {
            return ShareDecoded.failed(ShareStatus.TOO_LONG);
        }
        if (s.startsWith(Pw2Tables.FORMAT)) {
            StringBuilder symbols = new StringBuilder(compact.length());
            for (int i = Pw2Tables.FORMAT.length(); i < compact.length(); i++) {
                char c = compact.charAt(i);
                if (c != '-') {
                    symbols.append(c);
                }
            }
            return Pw2Codec.decode(symbols, lookup, aliases);
        }
        if (!s.startsWith(ShareTables.FORMAT)) {
            return ShareDecoded.failed(newerFormat(s) ? ShareStatus.UNSUPPORTED_VERSION : ShareStatus.INVALID_PREFIX);
        }
        StringBuilder symbols = new StringBuilder(s.length());
        for (int i = ShareTables.FORMAT.length(); i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '-') {
                symbols.append(c);
            }
        }
        byte[] frame = Base32.decode(symbols);
        if (frame.length > ShareTables.MAX_FRAME_BYTES) {
            return ShareDecoded.failed(ShareStatus.TOO_LARGE);
        }
        if (frame.length < ShareTables.MIN_FRAME_BYTES) {
            return ShareDecoded.failed(ShareStatus.TRUNCATED);
        }
        int bodyEnd = frame.length - 4;
        CRC32 crc = new CRC32();
        crc.update(frame, 0, bodyEnd);
        long stored = ((long) (frame[bodyEnd] & 0xFF) << 24) | ((long) (frame[bodyEnd + 1] & 0xFF) << 16)
                | ((long) (frame[bodyEnd + 2] & 0xFF) << 8) | (frame[bodyEnd + 3] & 0xFF);
        if (crc.getValue() != stored) {
            return ShareDecoded.failed(ShareStatus.CHECKSUM);
        }
        int version = frame[0] & 0xFF;
        int flags = frame[1] & 0xFF;
        if (version != ShareTables.FRAME_VERSION || (flags & ~ShareTables.FLAG_DEFLATE) != 0) {
            return ShareDecoded.failed(ShareStatus.UNSUPPORTED_VERSION);
        }
        byte[] payload = Arrays.copyOfRange(frame, 2, bodyEnd);
        if ((flags & ShareTables.FLAG_DEFLATE) != 0) {
            payload = inflate(payload);
        } else if (payload.length > ShareTables.MAX_PAYLOAD_BYTES) {
            return ShareDecoded.failed(ShareStatus.TOO_LARGE);
        }
        return parse(payload, lookup, aliases);
    }

    private static boolean newerFormat(String upper) {
        int end = 2;
        while (end < upper.length() && end < 5 && upper.charAt(end) >= '0' && upper.charAt(end) <= '9') {
            end++;
        }
        return upper.startsWith("PW") && end > 2 && Integer.parseInt(upper.substring(2, end)) > 2;
    }

    private static byte[] inflate(byte[] data) throws ShareFormatException {
        return inflate(data, null);
    }

    static byte[] inflate(byte[] data, byte[] dictionary) throws ShareFormatException {
        Inflater inflater = new Inflater(true);
        try {
            if (dictionary != null) {
                inflater.setDictionary(dictionary);
            }
            byte[] input = Arrays.copyOf(data, data.length + 1);
            inflater.setInput(input);
            byte[] out = new byte[ShareTables.MAX_PAYLOAD_BYTES + 1];
            int total = 0;
            while (!inflater.finished()) {
                if (total >= out.length) {
                    throw new ShareFormatException(ShareStatus.TOO_LARGE);
                }
                int n = inflater.inflate(out, total, out.length - total);
                if (n == 0 && !inflater.finished()) {
                    throw new ShareFormatException(ShareStatus.CORRUPT);
                }
                total += n;
            }
            if (total > ShareTables.MAX_PAYLOAD_BYTES) {
                throw new ShareFormatException(ShareStatus.TOO_LARGE);
            }
            if (inflater.getRemaining() > 1) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            return Arrays.copyOf(out, total);
        } catch (DataFormatException e) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        } finally {
            inflater.end();
        }
    }

    private static ShareDecoded parse(byte[] payload, PartLookup lookup, Map<Identifier, Identifier> aliases) throws ShareFormatException {
        ByteReader reader = new ByteReader(payload);
        long mask = reader.varint();
        if ((mask & ShareLayer.PARTS.mask()) == 0) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        ShareParsed parsed = new ShareParsed();
        for (int bit = 0; bit < 32; bit++) {
            if ((mask & (1L << bit)) == 0) {
                continue;
            }
            ByteReader block = reader.block();
            Optional<ShareLayer> layer = ShareLayer.byBit(bit);
            if (layer.isEmpty()) {
                parsed.skipped.add("layer:" + bit);
                continue;
            }
            parsed.layers.add(layer.get());
            switch (layer.get()) {
                case PARTS -> readParts(block, parsed);
                case PAINT -> readPaint(block, parsed);
                case TUNE -> readTune(block, parsed);
                case OSD -> readOsd(block, parsed);
                case NAME -> readName(block, parsed);
                default -> throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            if (!block.atEnd()) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
        }
        if (!reader.atEnd()) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        return finish(parsed, lookup, aliases);
    }

    static ShareDecoded finish(ShareParsed parsed, PartLookup lookup, Map<Identifier, Identifier> aliases) throws ShareFormatException {
        BuildSpec build = applyAliases(parsed, aliases);
        ShareContent content = new ShareContent(build, Optional.ofNullable(parsed.paint), Optional.ofNullable(parsed.tune),
                Optional.ofNullable(parsed.osd), Optional.ofNullable(parsed.name));
        LinkedHashSet<Identifier> unknown = new LinkedHashSet<>();
        LinkedHashSet<Identifier> wrong = new LinkedHashSet<>();
        classify(build.frame(), PartKind.FRAME, lookup, unknown, wrong);
        classify(build.stack(), PartKind.STACK, lookup, unknown, wrong);
        classify(build.motor(), PartKind.MOTOR, lookup, unknown, wrong);
        classify(build.prop(), PartKind.PROP, lookup, unknown, wrong);
        classify(build.video(), PartKind.VIDEO, lookup, unknown, wrong);
        classify(build.battery(), PartKind.BATTERY, lookup, unknown, wrong);
        for (AccessorySlot slot : ShareTables.ACCESSORY_SLOTS) {
            Optional<Identifier> accessory = build.accessory(slot);
            if (accessory.isPresent()) {
                classify(accessory.get(), PartKind.ACCESSORY, lookup, unknown, wrong);
            }
        }
        return new ShareDecoded(ShareStatus.OK, Optional.of(content), parsed.layers, List.copyOf(unknown), List.copyOf(wrong),
                List.copyOf(parsed.renamed.values()), parsed.skipped);
    }

    private static void classify(Identifier id, PartKind expected, PartLookup lookup, LinkedHashSet<Identifier> unknown,
                                 LinkedHashSet<Identifier> wrong) {
        if (ShareTables.unknownNamespaceIndex(id).isPresent()) {
            unknown.add(id);
            return;
        }
        Optional<PartKind> kind;
        try {
            kind = lookup.kindOf(id);
        } catch (RuntimeException e) {
            kind = Optional.empty();
        }
        if (kind.isEmpty()) {
            unknown.add(id);
        } else if (kind.get() != expected) {
            wrong.add(id);
        }
    }

    private static BuildSpec applyAliases(ShareParsed parsed, Map<Identifier, Identifier> aliases) throws ShareFormatException {
        if (parsed.parts == null) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        Identifier[] ids = parsed.parts;
        for (int i = 0; i < ids.length; i++) {
            ids[i] = alias(ids[i], aliases, parsed);
        }
        EnumMap<AccessorySlot, Identifier> accessories = new EnumMap<>(AccessorySlot.class);
        for (Map.Entry<AccessorySlot, Identifier> entry : parsed.accessories.entrySet()) {
            accessories.put(entry.getKey(), alias(entry.getValue(), aliases, parsed));
        }
        return new BuildSpec(ids[0], ids[1], ids[2], ids[3], ids[4], ids[5], accessories);
    }

    private static Identifier alias(Identifier id, Map<Identifier, Identifier> aliases, ShareParsed parsed) {
        if (ShareTables.unknownNamespaceIndex(id).isPresent()) {
            return id;
        }
        Identifier resolved = PartAliases.resolve(id, aliases);
        if (!resolved.equals(id)) {
            parsed.renamed.putIfAbsent(id, new PartAliases.Rename(id, resolved));
        }
        return resolved;
    }

    private static void readParts(ByteReader block, ShareParsed parsed) throws ShareFormatException {
        Identifier[] ids = new Identifier[6];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = readRef(block, parsed);
        }
        int count = block.u8();
        for (int i = 0; i < count; i++) {
            int slot = block.u8();
            Identifier id = readRef(block, parsed);
            if (slot >= ShareTables.ACCESSORY_SLOTS.size()) {
                parsed.skipped.add("accessory_slot:" + slot);
                continue;
            }
            AccessorySlot key = ShareTables.ACCESSORY_SLOTS.get(slot);
            if (parsed.accessories.containsKey(key)) {
                parsed.skipped.add("accessory_slot_duplicate:" + slot);
            }
            parsed.accessories.put(key, id);
        }
        parsed.parts = ids;
    }

    private static Identifier readRef(ByteReader block, ShareParsed parsed) throws ShareFormatException {
        long ns = block.varint();
        String namespace = null;
        if (ns == 0) {
            namespace = block.str();
            if (!ShareTables.validNamespace(namespace)) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
        } else if (ns < ShareTables.NAMESPACES.size()) {
            namespace = ShareTables.NAMESPACES.get((int) ns);
        }
        String path = block.str();
        if (!ShareTables.validPath(path)) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        if (namespace == null) {
            String marker = "namespace:" + ns;
            if (!parsed.skipped.contains(marker)) {
                parsed.skipped.add(marker);
            }
            return ShareTables.unknownNamespace(ns, path);
        }
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    private static void readPaint(ByteReader block, ShareParsed parsed) throws ShareFormatException {
        int bits = block.u16();
        LinkedHashMap<String, Integer> colors = new LinkedHashMap<>();
        for (int i = 0; i < ShareTables.PAINT_SLOT_BITS; i++) {
            if ((bits & (1 << i)) == 0) {
                continue;
            }
            int rgb = (block.u8() << 16) | (block.u8() << 8) | block.u8();
            if (i < ShareTables.PAINT_SLOTS.size()) {
                colors.put(ShareTables.PAINT_SLOTS.get(i), rgb);
            } else {
                parsed.skipped.add("paint_slot:" + i);
            }
        }
        parsed.paint = new PaintSpec(colors);
    }

    private static void readTune(ByteReader block, ShareParsed parsed) throws ShareFormatException {
        long count = block.varint();
        if (count > ShareTables.MAX_TUNE_ENTRIES) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        TreeMap<String, Float> values = new TreeMap<>();
        for (long i = 0; i < count; i++) {
            long head = block.varint();
            long index = head >>> 1;
            boolean raw = (head & 1) != 0;
            String key;
            if (index == 0) {
                key = block.str();
                if (key.isEmpty() || key.length() > ShareTables.MAX_TUNE_KEY_CHARS) {
                    throw new ShareFormatException(ShareStatus.CORRUPT);
                }
            } else {
                key = ShareTables.tuneKey(index > Integer.MAX_VALUE ? 0 : (int) index).orElse(null);
            }
            float value = raw ? block.f32() : (float) (block.zigzag() / 1000.0);
            if (!Float.isFinite(value)) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            if (key == null) {
                parsed.skipped.add("tune_index:" + index);
                continue;
            }
            if (!ShareTables.sharedTuneKey(key)) {
                parsed.skipped.add("tune_key:" + key);
                continue;
            }
            if (values.containsKey(key)) {
                parsed.skipped.add("tune_duplicate:" + key);
            }
            values.put(key, value);
        }
        parsed.tune = new TuneSpec(values);
    }

    private static void readOsd(ByteReader block, ShareParsed parsed) throws ShareFormatException {
        long version = block.varint();
        String json = block.str();
        if (version < 1 || version > Integer.MAX_VALUE || json.length() > ShareTables.MAX_OSD_CHARS) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        parsed.osd = new ShareOsd((int) version, json);
    }

    private static void readName(ByteReader block, ShareParsed parsed) throws ShareFormatException {
        String name = block.str().strip();
        if (name.isEmpty() || name.length() > ShareTables.MAX_NAME_CHARS) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        parsed.name = name;
    }
}
