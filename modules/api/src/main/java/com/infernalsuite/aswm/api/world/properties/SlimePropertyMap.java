package com.infernalsuite.aswm.api.world.properties;

import lombok.Getter;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.HashMap;
import java.util.Map;

/**
 * A Property Map object.
 */
@Getter
public class SlimePropertyMap {

    private final Map<String, BinaryTag> properties;

    public SlimePropertyMap() {
        this(new HashMap<>());
    }

    public SlimePropertyMap(Map<String, BinaryTag> compoundMap) {
        this.properties = compoundMap;
    }

    /**
     * Return the current value of the given property
     *
     * @param property The slime property
     * @return The current value
     */
    public <T, Z extends BinaryTag> T getValue(SlimeProperty<T, Z> property) {
        if (properties.containsKey(property.getKey())) {
            return property.readValue(property.cast(properties.get(property.getKey())));
        } else {
            return property.getDefaultValue();
        }
    }

    /**
     * Update the value of the given property
     *
     * @param property The slime property
     * @param value    The new value
     * @throws IllegalArgumentException if the value fails validation.
     */
    public <T, Z extends BinaryTag> void setValue(SlimeProperty<T, Z> property, T value) {
        if (!property.applyValidator(value))
            throw new IllegalArgumentException("'%s' is not a valid property value.".formatted(value));

        this.properties.put(property.getKey(), property.createTag(value));
    }

    /**
     * Copies all values from the specified {@link SlimePropertyMap}.
     * If the same property has different values on both maps, the one
     * on the providen map will be used.
     *
     * @param propertyMap A {@link SlimePropertyMap}.
     */
    public void merge(SlimePropertyMap propertyMap) {
        properties.putAll(propertyMap.properties);
    }

    /**
     * Returns a {@link CompoundBinaryTag} containing every property set in this map.
     *
     * @return A {@link CompoundBinaryTag} with all the properties stored in this map.
     */
    public CompoundBinaryTag toCompound() {
        return CompoundBinaryTag.builder().put(properties).build();
    }

    public static SlimePropertyMap fromCompound(CompoundBinaryTag tag) {
        Map<String, BinaryTag> tags = new HashMap<>(tag.keySet().size());
        tag.forEach(entry -> tags.put(entry.getKey(), entry.getValue()));
        return new SlimePropertyMap(tags);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public SlimePropertyMap clone() {
        return new SlimePropertyMap(new HashMap<>(this.properties));
    }

    @Override
    public String toString() {
        return "SlimePropertyMap" + properties;
    }

}
