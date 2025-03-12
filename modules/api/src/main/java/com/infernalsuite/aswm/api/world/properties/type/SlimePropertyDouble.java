package com.infernalsuite.aswm.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class SlimePropertyDouble extends SlimeProperty<Double, DoubleBinaryTag> {

    public static SlimePropertyDouble create(final @NotNull String key, final double defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyDouble(key, defaultValue);
    }

    public static SlimePropertyDouble create(final @NotNull String key, final double defaultValue, final @NotNull Predicate<Double> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyDouble#create(String, double) instead");
        return new SlimePropertyDouble(key, defaultValue, validator);
    }

    private SlimePropertyDouble(String key, Double defaultValue) {
        super(key, defaultValue);
    }

    private SlimePropertyDouble(String key, Double defaultValue, Predicate<Double> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected DoubleBinaryTag createTag(final Double value) {
        return DoubleBinaryTag.of(value);
    }

    @Override
    protected Double readValue(final DoubleBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected DoubleBinaryTag cast(BinaryTag rawTag) {
        return (DoubleBinaryTag) rawTag;
    }
}
