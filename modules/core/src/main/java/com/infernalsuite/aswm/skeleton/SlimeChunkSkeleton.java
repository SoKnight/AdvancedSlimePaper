package com.infernalsuite.aswm.skeleton;

import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.List;

public record SlimeChunkSkeleton(
        int x, int z,
        SlimeChunkSection[] sections,
        CompoundBinaryTag heightMaps,
        int[] biomes,
        List<CompoundBinaryTag> tileEntities,
        List<CompoundBinaryTag> entities,
        CompoundBinaryTag extraData
) implements SlimeChunk {

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getZ() {
        return this.z;
    }

    @Override
    public SlimeChunkSection[] getSections() {
        return this.sections;
    }

    @Override
    public CompoundBinaryTag getHeightMaps() {
        return this.heightMaps;
    }

    @Override
    public int[] getBiomes() {
        return this.biomes;
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        return this.tileEntities;
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        return this.entities;
    }

    @Override
    public CompoundBinaryTag getExtraData() {
        return this.extraData;
    }

}
