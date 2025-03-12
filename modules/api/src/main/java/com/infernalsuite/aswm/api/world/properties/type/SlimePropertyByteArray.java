package com.infernalsuite.aswm.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class SlimePropertyByteArray extends SlimeProperty<byte[], ByteArrayBinaryTag> {

    public static SlimePropertyByteArray create(final @NotNull String key, final byte[] defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyByteArray(key, defaultValue);
    }

    public static SlimePropertyByteArray create(final @NotNull String key, final byte[] defaultValue, final @NotNull Predicate<byte[]> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyByteArray#create(String, byte[]) instead");
        return new SlimePropertyByteArray(key, defaultValue, validator);
    }

    private SlimePropertyByteArray(String key, byte[] defaultValue) {
        super(key, defaultValue);
    }

    private SlimePropertyByteArray(String key, byte[] defaultValue, Predicate<byte[]> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected ByteArrayBinaryTag createTag(final byte[] value) {
        return ByteArrayBinaryTag.of(value);
    }

    @Override
    protected byte[] readValue(final ByteArrayBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected ByteArrayBinaryTag cast(BinaryTag rawTag) {
        return (ByteArrayBinaryTag) rawTag;
    }

}

