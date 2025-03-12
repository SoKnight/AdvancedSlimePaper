package com.infernalsuite.aswm.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * A slime property of type integer
 */
public class SlimePropertyString extends SlimeProperty<String, StringBinaryTag> {

	public static SlimePropertyString create(final @NotNull String key, final String defaultValue) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		return new SlimePropertyString(key, defaultValue);
	}

	public static SlimePropertyString create(final @NotNull String key, final String defaultValue, final @NotNull Predicate<String> validator) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		Preconditions.checkNotNull(validator, "Use SlimePropertyString#create(String, String) instead");
		return new SlimePropertyString(key, defaultValue, validator);
	}

	private SlimePropertyString(String key, String defaultValue) {
		super(key, defaultValue);
	}

	private SlimePropertyString(String key, String defaultValue, Predicate<String> validator) {
		super(key, defaultValue, validator);
	}

	@Override
	protected StringBinaryTag createTag(final String value) {
		return StringBinaryTag.of(value);
	}

	@Override
	protected String readValue(final StringBinaryTag tag) {
		return tag.value();
	}

	@Override
	protected StringBinaryTag cast(BinaryTag rawTag) {
		return (StringBinaryTag) rawTag;
	}

}
