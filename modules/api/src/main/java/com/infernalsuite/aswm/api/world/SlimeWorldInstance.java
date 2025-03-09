package com.infernalsuite.aswm.api.world;

import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.nbt.BinaryTag;
import org.bukkit.World;

import java.util.concurrent.ConcurrentMap;

public interface SlimeWorldInstance {

    String getName();

    World getBukkitWorld();

    SlimeWorld getSlimeWorldMirror();

    SlimePropertyMap getPropertyMap();

    boolean isReadOnly();

    SlimeLoader getLoader();

    ConcurrentMap<String, BinaryTag> getExtraData();

}
