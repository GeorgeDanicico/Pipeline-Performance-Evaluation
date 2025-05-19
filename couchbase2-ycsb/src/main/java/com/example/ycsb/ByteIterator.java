package com.example.ycsb;

import java.io.IOException;

public abstract class ByteIterator {
    public abstract boolean hasNext();
    public abstract byte nextByte();
    public abstract long bytesLeft();
    public abstract void reset();

    public String toString() {
        StringBuilder sb = new StringBuilder();
        while (hasNext()) {
            sb.append((char)nextByte());
        }
        reset();
        return sb.toString();
    }

    public byte[] toArray() {
        long left = bytesLeft();
        if (left > Integer.MAX_VALUE) {
            throw new ArrayIndexOutOfBoundsException("Too much data to fit in one array");
        }
        byte[] ret = new byte[(int)left];
        int idx = 0;
        while (hasNext()) {
            ret[idx++] = nextByte();
        }
        reset();
        return ret;
    }
}