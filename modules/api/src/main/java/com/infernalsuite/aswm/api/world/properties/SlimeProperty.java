package com.infernalsuite.aswm.api.world.properties;

import lombok.Getter;
import net.kyori.adventure.nbt.BinaryTag;

import java.util.function.Predicate;

/**
 * A property describing behavior of a slime world.
 */
@Getter
public abstract class SlimeProperty<T, Z extends BinaryTag> {

    private final String key;
    private final T defaultValue;
    private final Predicate<T> validator;

    protected SlimeProperty(String key, T defaultValue) {
        this(key, defaultValue, null);
    }

    protected SlimeProperty(String key, T defaultValue, Predicate<T> validator) {
        this.key = key;

        if (defaultValue != null && validator != null && !validator.test(defaultValue))
            throw new IllegalArgumentException("Invalid default value for property '%s': '%s'".formatted(key, defaultValue));

        this.defaultValue = defaultValue;
        this.validator = validator;
    }

    protected abstract Z createTag(T value);

    protected abstract T readValue(Z tag);

    protected abstract Z cast(BinaryTag rawTag);

    public final boolean applyValidator(T value) {
        return validator == null || validator.test(value);
    }

    @Override
    public final String toString() {
        return "SlimeProperty{" +
                "key='" + key + '\'' +
                ", defaultValue=" + defaultValue +
                '}';
    }

}
