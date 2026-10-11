package de.lardtom.justmoreparts.share;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

final class ByteReader {
    private final byte[] data;
    private final int end;
    private int pos;

    ByteReader(byte[] data) {
        this(data, 0, data.length);
    }

    private ByteReader(byte[] data, int start, int end) {
        this.data = data;
        this.pos = start;
        this.end = end;
    }

    int remaining() {
        return this.end - this.pos;
    }

    boolean atEnd() {
        return this.pos == this.end;
    }

    int u8() throws ShareFormatException {
        if (this.pos >= this.end) {
            throw new ShareFormatException(ShareStatus.TRUNCATED);
        }
        return this.data[this.pos++] & 0xFF;
    }

    int u16() throws ShareFormatException {
        return (this.u8() << 8) | this.u8();
    }

    long u32() throws ShareFormatException {
        return ((long) this.u8() << 24) | ((long) this.u8() << 16) | ((long) this.u8() << 8) | this.u8();
    }

    long varint() throws ShareFormatException {
        long value = 0;
        for (int i = 0; i < 5; i++) {
            int b = this.u8();
            value |= (long) (b & 0x7F) << (7 * i);
            if ((b & 0x80) == 0) {
                if (value > 0xFFFFFFFFL) {
                    throw new ShareFormatException(ShareStatus.CORRUPT);
                }
                return value;
            }
        }
        throw new ShareFormatException(ShareStatus.CORRUPT);
    }

    long zigzag() throws ShareFormatException {
        long raw = this.varint();
        return (raw >>> 1) ^ -(raw & 1);
    }

    float f32() throws ShareFormatException {
        return Float.intBitsToFloat((int) this.u32());
    }

    int length() throws ShareFormatException {
        long length = this.varint();
        if (length > this.remaining()) {
            throw new ShareFormatException(ShareStatus.TRUNCATED);
        }
        return (int) length;
    }

    ByteReader block() throws ShareFormatException {
        int length = this.length();
        ByteReader inner = new ByteReader(this.data, this.pos, this.pos + length);
        this.pos += length;
        return inner;
    }

    String str() throws ShareFormatException {
        int length = this.length();
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(this.data, this.pos, length))
                    .toString();
            this.pos += length;
            return text;
        } catch (CharacterCodingException e) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
    }
}
