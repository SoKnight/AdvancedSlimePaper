package com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class v1_9SlimeWorld {

    public byte version;
    public final String worldName;
    public final SlimeLoader loader;
    public final Long2ObjectMap<v1_9SlimeChunk> chunks;
    public final CompoundTag extraCompound;
    public final SlimePropertyMap propertyMap;
    public final boolean readOnly;


}
