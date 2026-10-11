package de.lardtom.justmoreparts.share;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

final class ByteWriter {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    ByteWriter u8(int value) {
        this.out.write(value & 0xFF);
        return this;
    }

    ByteWriter u16(int value) {
        this.out.write((value >>> 8) & 0xFF);
        this.out.write(value & 0xFF);
        return this;
    }

    ByteWriter u32(long value) {
        this.out.write((int) (value >>> 24) & 0xFF);
        this.out.write((int) (value >>> 16) & 0xFF);
        this.out.write((int) (value >>> 8) & 0xFF);
        this.out.write((int) value & 0xFF);
        return this;
    }

    ByteWriter varint(long value) {
        if (value < 0 || value > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("varint außerhalb von 0 bis 2^32-1: " + value);
        }
        long rest = value;
        while (true) {
            int b = (int) (rest & 0x7F);
            rest >>>= 7;
            if (rest != 0) {
                this.out.write(b | 0x80);
            } else {
                this.out.write(b);
                return this;
            }
        }
    }

    ByteWriter zigzag(long value) {
        return this.varint((value << 1) ^ (value >> 63));
    }

    ByteWriter f32(float value) {
        return this.u32(Float.floatToRawIntBits(value) & 0xFFFFFFFFL);
    }

    ByteWriter str(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        this.varint(bytes.length);
        this.out.writeBytes(bytes);
        return this;
    }

    ByteWriter bytes(byte[] data) {
        this.out.writeBytes(data);
        return this;
    }

    ByteWriter block(byte[] data) {
        this.varint(data.length);
        return this.bytes(data);
    }

    int size() {
        return this.out.size();
    }

    byte[] toByteArray() {
        return this.out.toByteArray();
    }
}
