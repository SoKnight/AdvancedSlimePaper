package com.infernalsuite.aswm.api.world;

import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.World;

public interface SlimeWorldInstance {

    String getName();

    SlimeLoader getLoader();

    SlimePropertyMap getProperties();

    World getBukkitWorld();

    SlimeWorld getMirror();

    boolean isReadOnly();

    CompoundBinaryTag getExtraData();

}
