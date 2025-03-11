package com.infernalsuite.aswm.api.world;

import com.infernalsuite.aswm.api.utils.NibbleArray;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory representation of a SRF chunk section.
 */
public interface SlimeChunkSection {

    /**
     * Returns the block palette of the chunk section.
     *
     * @return The block palette, contained inside a {@link ListBinaryTag}
     */
    @NotNull ListBinaryTag getBlockPalette();

    /**
     * Returns all the states of the blocks of the chunk section.
     *
     * @return A <code>long[]</code> with every block state.
     */
    @NotNull long[] getBlockStates();

    /**
     * Returns the block light data.
     *
     * @return A {@link NibbleArray} with the block light data.
     */
    @Nullable NibbleArray getBlockLight();

    /**
     * Returns the sky light data.
     *
     * @return A {@link NibbleArray} containing the sky light data.
     */
    @Nullable NibbleArray getSkyLight();

}
