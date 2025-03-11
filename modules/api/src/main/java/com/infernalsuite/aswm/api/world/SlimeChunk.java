package com.infernalsuite.aswm.api.world;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * In-memory representation of a SRF chunk.
 */
public interface SlimeChunk {

    /**
     * Returns the X coordinate of the chunk.
     *
     * @return X coordinate of the chunk.
     */
    int getX();

    /**
     * Returns the Z coordinate of the chunk.
     *
     * @return Z coordinate of the chunk.
     */
    int getZ();

    /**
     * Returns all the sections of the chunk.
     *
     * @return A {@link SlimeChunkSection} array.
     */
    @NotNull SlimeChunkSection[] getSections();

    /**
     * Returns the height maps of the chunk.
     *
     * @return A {@link CompoundBinaryTag} containing all the height maps of the chunk.
     */
    @NotNull CompoundBinaryTag getHeightMaps();

    /**
     * Returns all the biomes of the chunk.
     *
     * @return A <code>int[]</code> containing all the biomes of the chunk.
     */
    @Nullable int[] getBiomes();

    /**
     * Returns all the tile entities of the chunk.
     *
     * @return A {@link CompoundBinaryTag} containing all the tile entities of the chunk.
     */
    @NotNull @Unmodifiable List<CompoundBinaryTag> getTileEntities();

    /**
     * Returns all the entities of the chunk.
     *
     * @return A {@link CompoundBinaryTag} containing all the entities
     */
    @NotNull @Unmodifiable List<CompoundBinaryTag> getEntities();

    /**
     * Returns the extra data of the chunk.
     * Inside this {@link CompoundBinaryTag}
     * can be stored any information to then be retrieved later, as it's
     * saved alongside the chunk data.
     * <br>
     * <b>Beware, a compound tag under the key "ChunkBukkitValues" will be stored here.
     * It is used for storing chunk-based Bukkit PDC. Do not overwrite it.</b>
     *
     * @return A {@link CompoundBinaryTag} containing the extra data of the chunk,
     */
    @NotNull CompoundBinaryTag getExtraData();

}
