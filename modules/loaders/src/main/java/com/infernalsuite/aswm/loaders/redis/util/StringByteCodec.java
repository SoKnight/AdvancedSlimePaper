package com.infernalsuite.aswm.loaders.redis.util;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StringByteCodec implements RedisCodec<String, byte[]> {

    public static final StringByteCodec INSTANCE = new StringByteCodec();
    private static final byte[] EMPTY = new byte[0];

    @Override
    public String decodeKey(final ByteBuffer bytes) {
        return StandardCharsets.UTF_8.decode(bytes).toString();
    }

    @Override
    public byte[] decodeValue(final ByteBuffer bytes) {
        return getBytes(bytes);
    }

    @Override
    public ByteBuffer encodeKey(final String key) {
        return StandardCharsets.UTF_8.encode(key);
    }

    @Override
    public ByteBuffer encodeValue(final byte[] value) {
        return value != null ? ByteBuffer.wrap(value) : ByteBuffer.wrap(EMPTY);
    }

    private static byte[] getBytes(final ByteBuffer buffer) {
        final byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        return b;
    }

}