package de.lardtom.justmoreparts.share;

import de.lardtom.propwash.api.build.BuildSpec;
import de.lardtom.propwash.api.build.PaintSpec;
import de.lardtom.propwash.api.build.TuneSpec;
import de.lardtom.propwash.api.part.AccessorySlot;
import de.lardtom.propwash.api.part.PartKind;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.CRC32;

final class Pw2Codec {
    private static final double[] SCALES = {1.0, 10.0, 100.0, 1000.0};
    private static final int[] SCALE_ORDER = {0, 1, 2, 3};

    private Pw2Codec() {
    }

    static String encode(ShareContent content) {
        BitWriter out = new BitWriter();
        out.bit(content.paint().isPresent()).bit(content.tune().isPresent()).bit(content.osd().isPresent())
                .bit(content.name().isPresent()).bit(false);
        writeParts(out, content.build());
        content.paint().ifPresent(paint -> writePaint(out, paint));
        content.tune().ifPresent(tune -> writeTune(out, tune));
        content.osd().ifPresent(osd -> writeOsd(out, osd));
        content.name().ifPresent(name -> writeText(out, name));
        byte[] payload = out.toByteArray();
        byte[] frame = new byte[payload.length + Pw2Tables.CHECKSUM_BYTES];
        System.arraycopy(payload, 0, frame, 0, payload.length);
        int check = checksum(payload, payload.length);
        frame[payload.length] = (byte) (check >>> 8);
        frame[payload.length + 1] = (byte) check;
        return Pw2Tables.PREFIX + Base62.encode(frame);
    }

    private static int checksum(byte[] data, int length) {
        CRC32 crc = new CRC32();
        crc.update(data, 0, length);
        return (int) (crc.getValue() & 0xFFFF);
    }

    private static void writeParts(BitWriter out, BuildSpec build) {
        writeRef(out, PartKind.FRAME, build.frame());
        writeRef(out, PartKind.STACK, build.stack());
        writeRef(out, PartKind.MOTOR, build.motor());
        writeRef(out, PartKind.PROP, build.prop());
        writeRef(out, PartKind.VIDEO, build.video());
        writeRef(out, PartKind.BATTERY, build.battery());
        for (AccessorySlot slot : build.accessories().keySet()) {
            if (!ShareTables.ACCESSORY_SLOTS.contains(slot)) {
                throw new ShareException("justmoreparts.share.error.accessory_slot", slot.key());
            }
        }
        List<AccessorySlot> used = new ArrayList<>();
        for (AccessorySlot slot : ShareTables.ACCESSORY_SLOTS) {
            if (build.accessory(slot).isPresent()) {
                used.add(slot);
            }
        }
        out.golomb(used.size(), 0);
        for (AccessorySlot slot : used) {
            out.golomb(ShareTables.accessoryIndex(slot), 0);
            writeRef(out, PartKind.ACCESSORY, build.accessory(slot).orElseThrow());
        }
    }

    private static void writeRef(BitWriter out, PartKind kind, Identifier id) {
        List<String> table = Pw2Tables.parts(kind);
        String namespace = id.getNamespace();
        String path = id.getPath();
        if (namespace.equals(Pw2Tables.UNKNOWN_NAMESPACE)) {
            long value = unknownValue(kind, path);
            if (value <= table.size()) {
                throw new ShareException("justmoreparts.share.error.part_id", id.toString());
            }
            out.golomb(value, Pw2Tables.K_PART);
            return;
        }
        int index = table.indexOf(id.toString());
        if (index >= 0) {
            out.golomb(index + 1L, Pw2Tables.K_PART);
            return;
        }
        if (!ShareTables.validPath(path)) {
            throw new ShareException("justmoreparts.share.error.part_id", id.toString());
        }
        if (!ShareTables.validNamespace(namespace)) {
            throw new ShareException("justmoreparts.share.error.namespace", id.toString());
        }
        out.golomb(0, Pw2Tables.K_PART);
        writeText(out, namespace);
        writeText(out, path);
    }

    private static long unknownValue(PartKind kind, String path) {
        String head = Pw2Tables.kindKey(kind) + ".";
        if (!path.startsWith(head)) {
            return -1;
        }
        String digits = path.substring(head.length());
        if (digits.isEmpty() || digits.length() > 10 || digits.startsWith("0") || !digits.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return -1;
        }
        long value = Long.parseLong(digits);
        return value <= Integer.MAX_VALUE ? value : -1;
    }

    private static void writePaint(BitWriter out, PaintSpec paint) {
        for (String slot : paint.colors().keySet()) {
            if (ShareTables.paintBit(slot) < 0) {
                throw new ShareException("justmoreparts.share.error.paint_slot", slot);
            }
        }
        int length = 0;
        for (int i = 0; i < ShareTables.PAINT_SLOTS.size(); i++) {
            if (paint.color(ShareTables.PAINT_SLOTS.get(i)).isPresent()) {
                length = i + 1;
            }
        }
        out.golomb(length, Pw2Tables.K_PAINT_SLOTS);
        for (int i = 0; i < length; i++) {
            out.bit(paint.color(ShareTables.PAINT_SLOTS.get(i)).isPresent());
        }
        List<Integer> seen = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            Optional<Integer> color = paint.color(ShareTables.PAINT_SLOTS.get(i));
            if (color.isEmpty()) {
                continue;
            }
            int rgb = color.get() & 0xFFFFFF;
            if (!seen.isEmpty()) {
                int at = seen.indexOf(rgb);
                out.bit(at < 0);
                if (at >= 0) {
                    out.bits(at, indexBits(seen.size()));
                    continue;
                }
            }
            seen.add(rgb);
            int table = Pw2Tables.COLORS.indexOf(rgb);
            out.bit(table < 0);
            if (table >= 0) {
                out.golomb(table, Pw2Tables.K_COLOR);
            } else {
                out.bits(rgb, 24);
            }
        }
    }

    private static int indexBits(int count) {
        return count <= 1 ? 0 : 32 - Integer.numberOfLeadingZeros(count - 1);
    }

    private static void writeTune(BitWriter out, TuneSpec tune) {
        List<Map.Entry<String, Float>> indexed = new ArrayList<>();
        List<Map.Entry<String, Float>> literal = new ArrayList<>();
        for (Map.Entry<String, Float> entry : tune.values().entrySet()) {
            if (!ShareTables.sharedTuneKey(entry.getKey())) {
                continue;
            }
            (Pw2Tables.tuneIndex(entry.getKey()) > 0 ? indexed : literal).add(entry);
        }
        indexed.sort(Comparator.comparingInt(e -> Pw2Tables.tuneIndex(e.getKey())));
        literal.sort(Map.Entry.comparingByKey());
        List<Integer> seen = new ArrayList<>();
        out.golomb(indexed.size(), Pw2Tables.K_TUNE_COUNT);
        int previous = 0;
        for (Map.Entry<String, Float> entry : indexed) {
            int index = Pw2Tables.tuneIndex(entry.getKey());
            out.golomb(index - previous - 1L, Pw2Tables.K_TUNE_GAP);
            previous = index;
            writeValue(out, entry.getValue(), seen);
        }
        out.golomb(literal.size(), 0);
        for (Map.Entry<String, Float> entry : literal) {
            writeText(out, entry.getKey());
            writeValue(out, entry.getValue(), seen);
        }
    }

    private static void writeValue(BitWriter out, float value, List<Integer> seen) {
        int raw = Float.floatToRawIntBits(value);
        int at = seen.indexOf(raw);
        if (at >= 0) {
            out.bits(0b110, 3);
            out.bits(at, indexBits(seen.size()));
            return;
        }
        seen.add(raw);
        for (int scale : SCALE_ORDER) {
            long q = scaled(value, scale);
            if (q == Long.MIN_VALUE) {
                continue;
            }
            switch (scale) {
                case 0 -> out.bits(0b0, 1);
                case 1 -> out.bits(0b1110, 4);
                case 2 -> out.bits(0b10, 2);
                default -> out.bits(0b11110, 5);
            }
            out.golomb(q >= 0 ? 2 * q : -2 * q - 1, Pw2Tables.K_VALUE);
            return;
        }
        out.bits(0b11111, 5);
        out.bits(raw & 0xFFFFFFFFL, 32);
    }

    static long scaled(float value, int scale) {
        if (!Float.isFinite(value)) {
            return Long.MIN_VALUE;
        }
        double factor = SCALES[scale];
        double product = (double) value * factor;
        if (Math.abs(product) >= 2147483647.5) {
            return Long.MIN_VALUE;
        }
        long q = Math.round(product);
        if (Math.abs(q) >= (1L << 31)) {
            return Long.MIN_VALUE;
        }
        float back = (float) (q / factor);
        return Float.floatToRawIntBits(back) == Float.floatToRawIntBits(value) ? q : Long.MIN_VALUE;
    }

    private static void writeOsd(BitWriter out, ShareOsd osd) {
        out.golomb(osd.version() - 1L, 0);
        byte[] text = osd.json().getBytes(StandardCharsets.UTF_8);
        byte[] packed = ShareCodec.deflate(text, Pw2Tables.OSD_DICTIONARY);
        long packedBits = BitWriter.golombLength(packed.length, Pw2Tables.K_OSD_BYTES) + 8L * packed.length;
        if (packedBits < textBits(osd.json())) {
            out.bit(true);
            out.golomb(packed.length, Pw2Tables.K_OSD_BYTES);
            out.bytes(packed);
        } else {
            out.bit(false);
            writeText(out, osd.json());
        }
    }

    private static int textMode(String text) {
        boolean table = true;
        boolean ascii = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            table &= Pw2Tables.TEXT_ALPHABET.indexOf(c) >= 0;
            ascii &= c >= 0x20 && c <= 0x7E;
        }
        return table ? 0 : ascii ? 1 : 2;
    }

    private static long textBits(String text) {
        return switch (textMode(text)) {
            case 0 -> 1 + BitWriter.golombLength(text.length(), Pw2Tables.K_TEXT) + 6L * text.length();
            case 1 -> 2 + BitWriter.golombLength(text.length(), Pw2Tables.K_TEXT) + 7L * text.length();
            default -> {
                int bytes = text.getBytes(StandardCharsets.UTF_8).length;
                yield 2 + BitWriter.golombLength(bytes, Pw2Tables.K_TEXT) + 8L * bytes;
            }
        };
    }

    private static void writeText(BitWriter out, String text) {
        switch (textMode(text)) {
            case 0 -> {
                out.bits(0b0, 1).golomb(text.length(), Pw2Tables.K_TEXT);
                for (int i = 0; i < text.length(); i++) {
                    out.bits(Pw2Tables.TEXT_ALPHABET.indexOf(text.charAt(i)), 6);
                }
            }
            case 1 -> {
                out.bits(0b10, 2).golomb(text.length(), Pw2Tables.K_TEXT);
                for (int i = 0; i < text.length(); i++) {
                    out.bits(text.charAt(i) - 0x20, 7);
                }
            }
            default -> {
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                out.bits(0b11, 2).golomb(bytes.length, Pw2Tables.K_TEXT).bytes(bytes);
            }
        }
    }

    static ShareDecoded decode(CharSequence symbols, ShareCodec.PartLookup lookup, Map<Identifier, Identifier> aliases)
            throws ShareFormatException {
        byte[] frame = Base62.decode(symbols);
        if (frame.length > ShareTables.MAX_FRAME_BYTES) {
            throw new ShareFormatException(ShareStatus.TOO_LARGE);
        }
        if (frame.length < Pw2Tables.MIN_FRAME_BYTES) {
            throw new ShareFormatException(ShareStatus.TRUNCATED);
        }
        int body = frame.length - Pw2Tables.CHECKSUM_BYTES;
        int stored = ((frame[body] & 0xFF) << 8) | (frame[body + 1] & 0xFF);
        if (checksum(frame, body) != stored) {
            throw new ShareFormatException(ShareStatus.CHECKSUM);
        }
        BitReader in = new BitReader(frame, body);
        boolean paint = in.bit();
        boolean tune = in.bit();
        boolean osd = in.bit();
        boolean name = in.bit();
        boolean extensions = in.bit();
        ShareParsed parsed = new ShareParsed();
        parsed.layers.add(ShareLayer.PARTS);
        readParts(in, parsed);
        if (paint) {
            parsed.layers.add(ShareLayer.PAINT);
            readPaint(in, parsed);
        }
        if (tune) {
            parsed.layers.add(ShareLayer.TUNE);
            readTune(in, parsed);
        }
        if (osd) {
            parsed.layers.add(ShareLayer.OSD);
            readOsd(in, parsed);
        }
        if (name) {
            parsed.layers.add(ShareLayer.NAME);
            String text = readText(in, ShareTables.MAX_NAME_CHARS * 4).strip();
            if (text.isEmpty() || text.length() > ShareTables.MAX_NAME_CHARS) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            parsed.name = text;
        }
        if (extensions) {
            int count = in.golombInt(0, Pw2Tables.MAX_EXTENSIONS);
            for (int i = 0; i < count; i++) {
                int layer = Pw2Tables.FIRST_EXTENSION_LAYER + in.golombInt(0, 1024);
                long bits = in.golomb(Pw2Tables.K_EXTENSION_BITS);
                in.skip(bits);
                parsed.skipped.add("layer:" + layer);
            }
        }
        if (!in.paddingOnly()) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        return ShareCodec.finish(parsed, lookup, aliases);
    }

    private static void readParts(BitReader in, ShareParsed parsed) throws ShareFormatException {
        Identifier[] ids = new Identifier[6];
        List<PartKind> kinds = Pw2Tables.KINDS;
        for (int i = 0; i < ids.length; i++) {
            ids[i] = readRef(in, kinds.get(i), parsed);
        }
        int count = in.golombInt(0, Pw2Tables.MAX_ACCESSORIES);
        for (int i = 0; i < count; i++) {
            int slot = in.golombInt(0, Integer.MAX_VALUE);
            Identifier id = readRef(in, PartKind.ACCESSORY, parsed);
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

    private static Identifier readRef(BitReader in, PartKind kind, ShareParsed parsed) throws ShareFormatException {
        int value = in.golombInt(Pw2Tables.K_PART, Integer.MAX_VALUE);
        if (value == 0) {
            String namespace = readText(in, ShareTables.MAX_NAMESPACE_CHARS);
            String path = readText(in, ShareTables.MAX_PATH_CHARS);
            if (!ShareTables.validNamespace(namespace) || !ShareTables.validPath(path) || namespace.equals(Pw2Tables.UNKNOWN_NAMESPACE)) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            return Identifier.fromNamespaceAndPath(namespace, path);
        }
        List<String> table = Pw2Tables.parts(kind);
        if (value <= table.size()) {
            return Identifier.parse(table.get(value - 1));
        }
        String path = Pw2Tables.kindKey(kind) + "." + value;
        parsed.skipOnce("part_index:" + path);
        return Identifier.fromNamespaceAndPath(Pw2Tables.UNKNOWN_NAMESPACE, path);
    }

    private static void readPaint(BitReader in, ShareParsed parsed) throws ShareFormatException {
        int length = in.golombInt(Pw2Tables.K_PAINT_SLOTS, Pw2Tables.MAX_PAINT_SLOT_BITS);
        boolean[] present = new boolean[length];
        for (int i = 0; i < length; i++) {
            present[i] = in.bit();
        }
        List<Integer> seen = new ArrayList<>();
        LinkedHashMap<String, Integer> colors = new LinkedHashMap<>();
        for (int i = 0; i < length; i++) {
            if (!present[i]) {
                continue;
            }
            int rgb;
            if (!seen.isEmpty() && !in.bit()) {
                int at = (int) in.bits(indexBits(seen.size()));
                if (at >= seen.size()) {
                    throw new ShareFormatException(ShareStatus.CORRUPT);
                }
                rgb = seen.get(at);
            } else {
                if (in.bit()) {
                    rgb = (int) in.bits(24);
                } else {
                    int table = in.golombInt(Pw2Tables.K_COLOR, Integer.MAX_VALUE);
                    if (table >= Pw2Tables.COLORS.size()) {
                        throw new ShareFormatException(ShareStatus.CORRUPT);
                    }
                    rgb = Pw2Tables.COLORS.get(table);
                }
                seen.add(rgb);
            }
            if (i < ShareTables.PAINT_SLOTS.size()) {
                colors.put(ShareTables.PAINT_SLOTS.get(i), rgb);
            } else {
                parsed.skipped.add("paint_slot:" + i);
            }
        }
        parsed.paint = new PaintSpec(colors);
    }

    private static void readTune(BitReader in, ShareParsed parsed) throws ShareFormatException {
        int indexed = in.golombInt(Pw2Tables.K_TUNE_COUNT, ShareTables.MAX_TUNE_ENTRIES);
        TreeMap<String, Float> values = new TreeMap<>();
        List<Integer> seen = new ArrayList<>();
        long index = 0;
        for (int i = 0; i < indexed; i++) {
            index += in.golomb(Pw2Tables.K_TUNE_GAP) + 1;
            if (index > Integer.MAX_VALUE) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            float value = readValue(in, seen);
            if (index > Pw2Tables.TUNE_KEYS.size()) {
                parsed.skipped.add("tune_index:" + index);
                continue;
            }
            putTune(values, Pw2Tables.TUNE_KEYS.get((int) index - 1), value, parsed);
        }
        int literal = in.golombInt(0, ShareTables.MAX_TUNE_ENTRIES - indexed);
        for (int i = 0; i < literal; i++) {
            String key = readText(in, ShareTables.MAX_TUNE_KEY_CHARS);
            if (key.isEmpty() || key.length() > ShareTables.MAX_TUNE_KEY_CHARS) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            putTune(values, key, readValue(in, seen), parsed);
        }
        parsed.tune = new TuneSpec(values);
    }

    private static void putTune(TreeMap<String, Float> values, String key, float value, ShareParsed parsed) {
        if (!ShareTables.sharedTuneKey(key)) {
            parsed.skipped.add("tune_key:" + key);
            return;
        }
        if (values.containsKey(key)) {
            parsed.skipped.add("tune_duplicate:" + key);
        }
        values.put(key, value);
    }

    private static float readValue(BitReader in, List<Integer> seen) throws ShareFormatException {
        int scale;
        if (!in.bit()) {
            scale = 0;
        } else if (!in.bit()) {
            scale = 2;
        } else if (!in.bit()) {
            if (seen.isEmpty()) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            int at = (int) in.bits(indexBits(seen.size()));
            if (at >= seen.size()) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            return Float.intBitsToFloat(seen.get(at));
        } else if (!in.bit()) {
            scale = 1;
        } else if (!in.bit()) {
            scale = 3;
        } else {
            int raw = (int) in.bits(32);
            float value = Float.intBitsToFloat(raw);
            if (!Float.isFinite(value)) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
            seen.add(raw);
            return value;
        }
        long zigzag = in.golomb(Pw2Tables.K_VALUE);
        if (zigzag > 0xFFFFFFFFL) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        long q = (zigzag >>> 1) ^ -(zigzag & 1);
        float value = (float) (q / SCALES[scale]);
        seen.add(Float.floatToRawIntBits(value));
        return value;
    }

    private static void readOsd(BitReader in, ShareParsed parsed) throws ShareFormatException {
        long version = in.golomb(0) + 1;
        if (version > Integer.MAX_VALUE) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        String json;
        if (in.bit()) {
            int length = in.golombInt(Pw2Tables.K_OSD_BYTES, Integer.MAX_VALUE);
            if (8L * length > in.remaining()) {
                throw new ShareFormatException(ShareStatus.TRUNCATED);
            }
            byte[] packed = new byte[length];
            for (int i = 0; i < length; i++) {
                packed[i] = (byte) in.bits(8);
            }
            json = utf8(ShareCodec.inflate(packed, Pw2Tables.OSD_DICTIONARY));
        } else {
            json = readText(in, ShareTables.MAX_OSD_CHARS);
        }
        if (json.length() > ShareTables.MAX_OSD_CHARS) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        parsed.osd = new ShareOsd((int) version, json);
    }

    private static String readText(BitReader in, int maxSymbols) throws ShareFormatException {
        int mode = in.bit() ? (in.bit() ? 2 : 1) : 0;
        int width = mode == 0 ? 6 : mode == 1 ? 7 : 8;
        long count = in.golomb(Pw2Tables.K_TEXT);
        if (count * width > in.remaining()) {
            throw new ShareFormatException(ShareStatus.TRUNCATED);
        }
        if (count > (long) maxSymbols * (mode == 2 ? 4 : 1)) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        int n = (int) count;
        if (mode == 2) {
            byte[] bytes = new byte[n];
            for (int i = 0; i < n; i++) {
                bytes[i] = (byte) in.bits(8);
            }
            return utf8(bytes);
        }
        StringBuilder out = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int symbol = (int) in.bits(width);
            if (mode == 0) {
                if (symbol >= Pw2Tables.TEXT_ALPHABET.length()) {
                    throw new ShareFormatException(ShareStatus.CORRUPT);
                }
                out.append(Pw2Tables.TEXT_ALPHABET.charAt(symbol));
            } else {
                if (symbol > 0x7E - 0x20) {
                    throw new ShareFormatException(ShareStatus.CORRUPT);
                }
                out.append((char) (symbol + 0x20));
            }
        }
        return out.toString();
    }

    private static String utf8(byte[] bytes) throws ShareFormatException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
    }
}
