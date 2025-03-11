package com.infernalsuite.aswm.skeleton;

import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SlimeChunkSectionSkeleton(
        ListBinaryTag blockPalette,
        long[] blockStates,
        NibbleArray block,
        NibbleArray light
) implements SlimeChunkSection {

    @Override
    public @NotNull ListBinaryTag getBlockPalette() {
        return this.blockPalette;
    }

    @Override
    public @NotNull long[] getBlockStates() {
        return this.blockStates;
    }

    @Override
    public @Nullable NibbleArray getBlockLight() {
        return this.block;
    }

    @Override
    public @Nullable NibbleArray getSkyLight() {
        return this.light;
    }

}
