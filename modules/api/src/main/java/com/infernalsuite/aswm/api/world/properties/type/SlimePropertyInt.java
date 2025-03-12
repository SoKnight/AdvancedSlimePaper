package com.infernalsuite.aswm.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * A slime property of type integer
 */
public class SlimePropertyInt extends SlimeProperty<Integer, IntBinaryTag> {

	public static SlimePropertyInt create(final @NotNull String key, final int defaultValue) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		return new SlimePropertyInt(key, defaultValue);
	}

	public static SlimePropertyInt create(final @NotNull String key, final int defaultValue, final @NotNull Predicate<Integer> validator) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		Preconditions.checkNotNull(validator, "Use SlimePropertyInt#create(String, int) instead");
		return new SlimePropertyInt(key, defaultValue, validator);
	}

	private SlimePropertyInt(String key, Integer defaultValue) {
		super(key, defaultValue);
	}

	private SlimePropertyInt(String key, Integer defaultValue, Predicate<Integer> validator) {
		super(key, defaultValue, validator);
	}

	@Override
	protected IntBinaryTag createTag(final Integer value) {
		return IntBinaryTag.of(value);
	}

	@Override
	protected Integer readValue(final IntBinaryTag tag) {
		return tag.value();
	}

	@Override
	protected IntBinaryTag cast(BinaryTag rawTag) {
		return (IntBinaryTag) rawTag;
	}

}
