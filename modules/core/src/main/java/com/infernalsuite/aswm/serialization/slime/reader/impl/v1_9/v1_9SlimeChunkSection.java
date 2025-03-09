package com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class v1_9SlimeChunkSection {

    // Pre 1.13 block data
    public final byte[] blocks;
    public final NibbleArray data;

    // Post 1.13 block data
    public final ListTag<CompoundTag> palette;
    public final long[] blockStates;

    // Post 1.17 block data
    public CompoundTag blockStatesTag;
    public CompoundTag biomeTag;

    public final NibbleArray blockLight;
    public final NibbleArray skyLight;

}