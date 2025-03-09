package com.infernalsuite.aswm.api;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.SlimeWorldInstance;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.ServiceLoader;

@ApiStatus.Internal
public interface SlimeNMSBridge {

    // Overriding
    boolean loadOverworldOverride();

    boolean loadNetherOverride();

    boolean loadEndOverride();

    void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) throws IOException;

    SlimeWorldInstance loadInstance(SlimeWorld slimeWorld);

    SlimeWorldInstance getInstance(World world);

    // Will return new (fixed) instance
    SlimeWorld applyDataFixers(SlimeWorld world);

    int getCurrentVersion();

    static SlimeNMSBridge instance() {
        return Holder.INSTANCE;
    }

    void extractCraftPDC(PersistentDataContainer source, CompoundMap target);

    PersistentDataContainer extractCompoundMapIntoCraftPDC(CompoundMap source);

    @ApiStatus.Internal
    class Holder {

        private static final SlimeNMSBridge INSTANCE = ServiceLoader.load(
                SlimeNMSBridge.class,
                SlimeNMSBridge.class.getClassLoader()
        ).findFirst().orElseThrow(() -> new IllegalStateException("There is no Slime NMS Bridge service provider found!"));

    }

    CompoundTag convertChunkTo1_13(CompoundTag tag);

}
