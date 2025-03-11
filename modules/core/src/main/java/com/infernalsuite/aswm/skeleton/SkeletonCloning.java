package com.infernalsuite.aswm.skeleton;

import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkeletonCloning {

    public static SkeletonSlimeWorld fullClone(@NotNull String worldName, @NotNull SlimeWorld world, @Nullable SlimeLoader loader) {
        return new SkeletonSlimeWorld(
                worldName,
                loader == null ? world.getLoader() : loader,
                world.getProperties().clone(),
                cloneChunkStorage(world.getChunkStorage()),
                deepClone(world.getWorldMaps()),
                loader == null || world.isReadOnly(),
                world.getDataVersion(),
                cloneExtraData(world.getExtraData())
        );
    }

    public static SkeletonSlimeWorld weakCopy(SlimeWorld world) {
        Long2ObjectMap<SlimeChunk> chunkStorageCopy = new Long2ObjectOpenHashMap<>();
        for (SlimeChunk chunk : world.getChunkStorage())
            chunkStorageCopy.put(SlimeWorld.chunkPosition(chunk), chunk);

        return new SkeletonSlimeWorld(
                world.getName(),
                world.getLoader(),
                world.getProperties().clone(),
                chunkStorageCopy,
                deepClone(world.getWorldMaps()),
                world.isReadOnly(),
                world.getDataVersion(),
                cloneExtraData(world.getExtraData())
        );
    }


    private static Long2ObjectMap<SlimeChunk> cloneChunkStorage(Collection<SlimeChunk> slimeChunkMap) {
        Long2ObjectMap<SlimeChunk> chunkStorageCopy = new Long2ObjectOpenHashMap<>();

        for (SlimeChunk chunk : slimeChunkMap) {
            SlimeChunkSection[] sectionsCopy = new SlimeChunkSection[chunk.getSections().length];
            for (int i = 0; i < sectionsCopy.length; i++) {
                SlimeChunkSection original = chunk.getSections()[i];
                if (original == null || original.getBlockPalette() == null || original.getBlockStates() == null)
                    continue; // This shouldn't happen, yet it does, not gonna figure out why.

                ListBinaryTag blockPaletteCopy = ListBinaryTag.from(original.getBlockPalette().stream().toList());
                long[] blockStates = original.getBlockStates();
                long[] blockStatesCopy = Arrays.copyOf(blockStates, blockStates.length);

                NibbleArray blockLight = original.getBlockLight();
                NibbleArray skyLight = original.getSkyLight();

                sectionsCopy[i] = new SlimeChunkSectionSkeleton(
                        blockPaletteCopy,
                        blockStatesCopy,
                        blockLight != null ? blockLight.clone() : null,
                        skyLight != null ? skyLight.clone() : null
                );
            }

            int[] biomes = chunk.getBiomes();
            int[] biomesCopy = biomes != null ? Arrays.copyOf(biomes, biomes.length) : null;

            SlimeChunkSkeleton clonedChunk = new SlimeChunkSkeleton(
                    chunk.getX(), chunk.getZ(),
                    sectionsCopy,
                    CompoundBinaryTag.builder().put(chunk.getHeightMaps()).build(),
                    biomes,
                    deepClone(chunk.getTileEntities()),
                    deepClone(chunk.getEntities()),
                    CompoundBinaryTag.builder().put(chunk.getExtraData()).build()
            );

            chunkStorageCopy.put(SlimeWorld.chunkPosition(chunk), clonedChunk);
        }

        return chunkStorageCopy;
    }

    private static CompoundBinaryTag cloneExtraData(@Nullable CompoundBinaryTag extraData) {
        return extraData != null ? extraData : CompoundBinaryTag.empty();
    }

    private static List<CompoundBinaryTag> deepClone(List<CompoundBinaryTag> tags) {
        if (tags == null)
            return new ArrayList<>(0);

        List<CompoundBinaryTag> cloned = new ArrayList<>(tags.size());
        for (CompoundBinaryTag tag : tags)
            cloned.add(CompoundBinaryTag.builder().put(tag).build());

        return cloned;
    }

}
