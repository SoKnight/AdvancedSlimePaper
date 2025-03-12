package com.infernalsuite.aswm.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * A slime property of type float
 */
public class SlimePropertyFloat extends SlimeProperty<Float, FloatBinaryTag> {

    public static SlimePropertyFloat create(final @NotNull String key, final float defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyFloat(key, defaultValue);
    }

    public static SlimePropertyFloat create(final @NotNull String key, final float defaultValue, final @NotNull Predicate<Float> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyFloat#create(String, float) instead");
        return new SlimePropertyFloat(key, defaultValue, validator);
    }

    private SlimePropertyFloat(String key, Float defaultValue) {
        super(key, defaultValue);
    }

    private SlimePropertyFloat(String key, Float defaultValue, Predicate<Float> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected FloatBinaryTag createTag(final Float value) {
        return FloatBinaryTag.of(value);
    }

    @Override
    protected Float readValue(final FloatBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected FloatBinaryTag cast(BinaryTag rawTag) {
        return (FloatBinaryTag) rawTag;
    }

}
