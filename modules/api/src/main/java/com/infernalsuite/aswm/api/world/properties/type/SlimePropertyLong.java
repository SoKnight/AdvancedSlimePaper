package com.infernalsuite.aswm.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class SlimePropertyLong extends SlimeProperty<Long, LongBinaryTag> {

    public static SlimePropertyLong create(final @NotNull String key, final long defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyLong(key, defaultValue);
    }

    public static SlimePropertyLong create(final @NotNull String key, final long defaultValue, final @NotNull Predicate<Long> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyLong#create(String, long) instead");
        return new SlimePropertyLong(key, defaultValue, validator);
    }

    private SlimePropertyLong(String key, Long defaultValue) {
        super(key, defaultValue);
    }

    private SlimePropertyLong(String key, Long defaultValue, Predicate<Long> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected LongBinaryTag createTag(final Long value) {
        return LongBinaryTag.of(value);
    }

    @Override
    protected Long readValue(final LongBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected LongBinaryTag cast(BinaryTag rawTag) {
        return (LongBinaryTag) rawTag;
    }

}
