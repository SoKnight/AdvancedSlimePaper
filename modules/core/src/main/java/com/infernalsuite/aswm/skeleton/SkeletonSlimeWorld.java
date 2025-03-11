package com.infernalsuite.aswm.skeleton;

import com.infernalsuite.aswm.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.slime.SlimeSerializer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Getter
public final class SkeletonSlimeWorld implements SlimeWorld {

    private final String name;
    private final SlimeLoader loader;
    private final SlimePropertyMap properties;
    private final Long2ObjectMap<SlimeChunk> chunkStorage;
    private final List<CompoundBinaryTag> worldMaps;
    private final boolean readOnly;
    private final int dataVersion;
    private final CompoundBinaryTag extraData;

    public SkeletonSlimeWorld(
            String name,
            SlimeLoader loader,
            SlimePropertyMap properties,
            Long2ObjectMap<SlimeChunk> chunkStorage,
            List<CompoundBinaryTag> worldMaps,
            boolean readOnly,
            int dataVersion,
            CompoundBinaryTag extraData
    ) {
        this.name = name;
        this.loader = loader;
        this.properties = properties;
        this.chunkStorage = chunkStorage;
        this.worldMaps = worldMaps;
        this.readOnly = readOnly;
        this.dataVersion = dataVersion;
        this.extraData = extraData;
    }

    @Override
    public SlimeChunk getChunk(int x, int z) {
        return chunkStorage.get(SlimeWorld.chunkPosition(x, z));
    }

    @Override
    public void updateChunk(SlimeChunk chunk) {
        Objects.requireNonNull(chunk, "chunk cannot be null");
        this.chunkStorage.put(SlimeWorld.chunkPosition(chunk), chunk);
    }

    @Override
    public SlimeWorld clone(String worldName) {
        try {
            return clone(worldName, null);
        } catch (WorldAlreadyExistsException | IOException ignored) {
            return null; // Never going to happen
        }
    }

    @Override
    public @NotNull SlimeWorld clone(@NotNull String worldName, @NotNull SlimeLoader loader) throws WorldAlreadyExistsException, IOException {
        if (name.equals(worldName))
            throw new IllegalArgumentException("The clone world cannot have the same name as the original world!");

        if (worldName == null)
            throw new IllegalArgumentException("The world name cannot be null!");

        if (loader != null && loader.worldExists(worldName))
            throw new WorldAlreadyExistsException(worldName);

        SlimeWorld cloned = SkeletonCloning.fullClone(worldName, this, loader);
        if (loader != null)
            loader.saveWorld(worldName, SlimeSerializer.serialize(cloned));

        return cloned;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;

        SkeletonSlimeWorld that = (SkeletonSlimeWorld) o;
        return readOnly == that.readOnly
                && dataVersion == that.dataVersion
                && Objects.equals(name, that.name)
                && Objects.equals(loader, that.loader)
                && Objects.equals(properties, that.properties)
                && Objects.equals(chunkStorage, that.chunkStorage)
                && Objects.equals(worldMaps, that.worldMaps)
                && Objects.equals(extraData, that.extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, loader, properties, chunkStorage, worldMaps, readOnly, dataVersion, extraData);
    }

    @Override
    public String toString() {
        return "SkeletonSlimeWorld{" +
                "chunkStorage=" + chunkStorage +
                ", name='" + name + '\'' +
                ", loader=" + loader +
                ", properties=" + properties +
                ", worldMaps=" + worldMaps +
                ", readOnly=" + readOnly +
                ", dataVersion=" + dataVersion +
                ", extraData=" + extraData +
                '}';
    }

}
