package com.infernalsuite.aswm.api.world.properties;

import lombok.Getter;
import net.kyori.adventure.nbt.BinaryTag;

import java.util.function.Function;

/**
 * A property describing behavior of a slime world.
 */
@Getter
public abstract class SlimeProperty<T, Z extends BinaryTag> {

    private final String key;
    private final T defaultValue;
    private final Function<T, Boolean> validator;

    protected SlimeProperty(String key, T defaultValue) {
        this(key, defaultValue, null);
    }

    protected SlimeProperty(String key, T defaultValue, Function<T, Boolean> validator) {
        this.key = key;

        if (defaultValue != null && validator != null && !validator.apply(defaultValue)) {
            throw new IllegalArgumentException("Invalid default value for property " + key + "! " + defaultValue);
        }

        this.defaultValue = defaultValue;
        this.validator = validator;
    }

    protected abstract Z createTag(T value);

    protected abstract T readValue(Z tag);

    protected abstract Z cast(BinaryTag rawTag);

    public final boolean applyValidator(T value) {
        return this.validator != null && this.validator.apply(value);
    }

    @Override
    public final String toString() {
        return "SlimeProperty{" +
                "key='" + key + '\'' +
                ", defaultValue=" + defaultValue +
                '}';
    }

}
