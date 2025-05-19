package com.example.ycsb;

public class StringByteIterator extends ByteIterator {
    private byte[] str;
    private int off;

    public StringByteIterator(String s) {
        this.str = s.getBytes();
        this.off = 0;
    }

    @Override
    public boolean hasNext() {
        return off < str.length;
    }

    @Override
    public byte nextByte() {
        if (!hasNext()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return str[off++];
    }

    @Override
    public long bytesLeft() {
        return str.length - off;
    }

    @Override
    public void reset() {
        off = 0;
    }
}