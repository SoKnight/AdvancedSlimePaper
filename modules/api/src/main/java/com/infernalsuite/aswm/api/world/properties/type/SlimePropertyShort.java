package com.infernalsuite.aswm.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class SlimePropertyShort extends SlimeProperty<Short, ShortBinaryTag> {

    public static SlimePropertyShort create(final @NotNull String key, final short defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyShort(key, defaultValue);
    }

    public static SlimePropertyShort create(final @NotNull String key, final short defaultValue, final @NotNull Predicate<Short> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyShort#create(String, short) instead");
        return new SlimePropertyShort(key, defaultValue, validator);
    }

    private SlimePropertyShort(String key, Short defaultValue) {
        super(key, defaultValue);
    }

    private SlimePropertyShort(String key, Short defaultValue, Predicate<Short> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected ShortBinaryTag createTag(final Short value) {
        return ShortBinaryTag.of(value);
    }

    @Override
    protected Short readValue(final ShortBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected ShortBinaryTag cast(BinaryTag rawTag) {
        return (ShortBinaryTag) rawTag;
    }

}
