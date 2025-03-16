package com.infernalsuite.aswm.api.world;

import com.infernalsuite.aswm.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * In-memory representation of a SRF world.
 */
public interface SlimeWorld {

    /**
     * Returns the name of the world.
     *
     * @return The name of the world.
     */
    @NotNull String getName();

    /**
     * Returns the {@link SlimeLoader} used
     * to load and store the world.
     *
     * @return The {@link SlimeLoader} used to load and store the world.
     */
    @Nullable SlimeLoader getLoader();

    /**
     * Returns the property map.
     *
     * @return A {@link SlimePropertyMap} object containing all the properties of the world.
     */
    @NotNull SlimePropertyMap getProperties();

    /**
     * Returns the chunk that belongs to the coordinates specified.
     *
     * @param x X coordinate.
     * @param z Z coordinate.
     *
     * @return The {@link SlimeChunk} that belongs to those coordinates.
     */
    @Nullable SlimeChunk getChunk(int x, int z);

    /**
     * Returns a {@link Collection} with all world chunks.
     *
     * @return A {@link Collection} containing every world chunk.
     */
    @NotNull Collection<SlimeChunk> getChunkStorage();

    /**
     * Updates chunk in the world chunk storage.
     * @param chunk Chunk to update.
     */
    void updateChunk(@NotNull SlimeChunk chunk);

    /**
     * Returns whether read-only is enabled.
     *
     * @return true if read-only is enabled, false otherwise.
     */
    boolean isReadOnly();

    /**
     * Returns world version value used to identify a world format.
     *
     * @return The version of this world.
     */
    int getDataVersion();

    /**
     * Returns the extra data of the world. Inside this {@link CompoundBinaryTag}
     * can be stored any information to then be retrieved later, as it's
     * saved alongside the world data.
     *
     * @return A {@link CompoundBinaryTag} containing the extra data of the world.
     */
    @NotNull CompoundBinaryTag getExtraData();

    /**
     * Puts the provided extra data into the world.
     * @param extraData Extra data to put into.
     */
    @NotNull CompoundBinaryTag putExtraData(@Nullable CompoundBinaryTag extraData);

    /**
     * Updates the world extra data using provided customizer function.
     * @param compoundCustomizer Compound tag customizer function.
     */
    @NotNull CompoundBinaryTag updateExtraData(@NotNull Consumer<CompoundBinaryTag.Builder> compoundCustomizer);

    /**
     * Returns a clone of the world with the given name. This world will never be
     * stored, as the <code>readOnly</code> property will be set to true.
     *
     * @param worldName The name of the cloned world.
     *
     * @return The clone of the world.
     *
     * @throws IllegalArgumentException if the name of the world is the same as the current one or is <code>null</code>.
     */
    @NotNull SlimeWorld clone(@NotNull String worldName);

    /**
     * Returns a clone of the world with the given name. The world will be
     * automatically stored inside the provided data source.
     *
     * @param worldName The name of the cloned world.
     * @param loader The {@link SlimeLoader} used to store the world or <code>null</code> if the world is temporary.
     *
     * @return The clone of the world.
     *
     * @throws IllegalArgumentException if the name of the world is the same as the current one or is <code>null</code>.
     * @throws WorldAlreadyExistsException if there's already a world with the same name inside the provided data source.
     * @throws IOException if the world could not be stored.
     */
    @NotNull SlimeWorld clone(
            @NotNull String worldName,
            @Nullable SlimeLoader loader
    ) throws WorldAlreadyExistsException, IOException;

    static long chunkPosition(@NotNull SlimeChunk chunk) {
        return chunkPosition(chunk.getX(), chunk.getZ());
    }

    static long chunkPosition(int x, int z) {
        return ((((long) x) << 32) | (z & 0xFFFFFFFFL));
    }

}
