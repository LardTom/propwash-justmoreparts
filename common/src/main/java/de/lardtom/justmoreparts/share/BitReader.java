package de.lardtom.justmoreparts.share;

final class BitReader {
    private static final int MAX_GOLOMB_ZEROS = 40;

    private final byte[] data;
    private final long end;
    private long pos;

    BitReader(byte[] data, int byteLength) {
        this.data = data;
        this.end = (long) byteLength * 8;
    }

    long remaining() {
        return this.end - this.pos;
    }

    boolean bit() throws ShareFormatException {
        if (this.pos >= this.end) {
            throw new ShareFormatException(ShareStatus.TRUNCATED);
        }
        boolean value = (this.data[(int) (this.pos >>> 3)] & (0x80 >>> (this.pos & 7))) != 0;
        this.pos++;
        return value;
    }

    long bits(int count) throws ShareFormatException {
        if (count > this.remaining()) {
            throw new ShareFormatException(ShareStatus.TRUNCATED);
        }
        long value = 0;
        for (int i = 0; i < count; i++) {
            value = (value << 1) | (this.bit() ? 1 : 0);
        }
        return value;
    }

    long golomb(int k) throws ShareFormatException {
        int zeros = 0;
        while (!this.bit()) {
            zeros++;
            if (zeros > MAX_GOLOMB_ZEROS) {
                throw new ShareFormatException(ShareStatus.CORRUPT);
            }
        }
        long shifted = 1L;
        for (int i = 0; i < zeros + k; i++) {
            shifted = (shifted << 1) | (this.bit() ? 1 : 0);
        }
        return shifted - (1L << k);
    }

    int golombInt(int k, long max) throws ShareFormatException {
        long value = this.golomb(k);
        if (value > max) {
            throw new ShareFormatException(ShareStatus.CORRUPT);
        }
        return (int) value;
    }

    void skip(long count) throws ShareFormatException {
        if (count > this.remaining()) {
            throw new ShareFormatException(ShareStatus.TRUNCATED);
        }
        this.pos += count;
    }

    boolean paddingOnly() throws ShareFormatException {
        if (this.remaining() >= 8) {
            return false;
        }
        while (this.remaining() > 0) {
            if (this.bit()) {
                return false;
            }
        }
        return true;
    }
}
