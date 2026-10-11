package de.lardtom.justmoreparts.share;

import java.util.Arrays;

public final class Base32 {
    private static final int[] VALUES = new int[128];

    static {
        Arrays.fill(VALUES, -1);
        for (int i = 0; i < ShareTables.ALPHABET.length(); i++) {
            VALUES[ShareTables.ALPHABET.charAt(i)] = i;
        }
        VALUES['O'] = 0;
        VALUES['I'] = 1;
        VALUES['L'] = 1;
    }

    private Base32() {
    }

    public static String encode(byte[] data) {
        StringBuilder out = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                out.append(ShareTables.ALPHABET.charAt((buffer >>> bits) & 0x1F));
            }
            buffer &= (1 << bits) - 1;
        }
        if (bits > 0) {
            out.append(ShareTables.ALPHABET.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return out.toString();
    }

    public static String group(String symbols) {
        StringBuilder out = new StringBuilder(symbols.length() + symbols.length() / ShareTables.GROUP_LENGTH);
        for (int i = 0; i < symbols.length(); i += ShareTables.GROUP_LENGTH) {
            if (i > 0) {
                out.append('-');
            }
            out.append(symbols, i, Math.min(symbols.length(), i + ShareTables.GROUP_LENGTH));
        }
        return out.toString();
    }

    static byte[] decode(CharSequence symbols) throws ShareFormatException {
        int count = symbols.length();
        byte[] out = new byte[count * 5 / 8];
        int buffer = 0;
        int bits = 0;
        int index = 0;
        for (int i = 0; i < count; i++) {
            char c = symbols.charAt(i);
            int value = c < VALUES.length ? VALUES[c] : -1;
            if (value < 0) {
                throw new ShareFormatException(ShareStatus.BAD_CHARACTER);
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out[index++] = (byte) (buffer >>> bits);
                buffer &= (1 << bits) - 1;
            }
        }
        if (buffer != 0) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        return out;
    }
}
