package com.ririv.quickoutline.utils;

import java.io.OutputStream;
import java.util.Arrays;

/**
 * 一个高性能的 ByteArrayOutputStream，允许直接访问内部缓冲区以避免内存复制。
 */
public class FastByteArrayOutputStream extends OutputStream {
    private byte[] buf;
    private int count;

    public FastByteArrayOutputStream() {
        this(32);
    }

    public FastByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        buf = new byte[size];
    }

    @Override
    public void write(int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) - b.length > 0)) {
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity - buf.length > 0) {
            grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        if (newCapacity < 0) {
            if (minCapacity < 0) { // overflow
                throw new OutOfMemoryError();
            }
            newCapacity = Integer.MAX_VALUE;
        }
        buf = Arrays.copyOf(buf, newCapacity);
    }

    /**
     * 直接获取内部缓冲区引用。
     * 警告：返回的数组可能比实际数据大，必须配合 {@link #size()} 使用。
     * 修改返回的数组会影响 Stream 内容。
     */
    public byte[] getBuffer() {
        return buf;
    }

    public int size() {
        return count;
    }

    public void reset() {
        count = 0;
    }
}
