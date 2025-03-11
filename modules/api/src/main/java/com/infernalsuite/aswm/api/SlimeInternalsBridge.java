package com.infernalsuite.aswm.api;

import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.SlimeWorldInstance;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ServiceLoader;

@ApiStatus.Internal
public interface SlimeInternalsBridge {

    int getCurrentVersion();

    boolean loadOverworldOverride();

    boolean loadNetherOverride();

    boolean loadEndOverride();

    void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) throws IOException;

    SlimeWorldInstance getInstance(World world);

    SlimeWorldInstance loadInstance(SlimeWorld slimeWorld);

    // TODO apply datafixers via spottedleaf's dataconverter library which isn't bundled on 1.16.5
//    // Will return new (fixed) instance
//    SlimeWorld applyDataFixers(SlimeWorld world);

    PersistentDataContainer deserializeCraftPDC(CompoundBinaryTag source);

    void serializeCraftPDC(PersistentDataContainer source, CompoundBinaryTag.Builder builder);

    static @NotNull SlimeInternalsBridge get() {
        return Holder.INSTANCE;
    }

    @ApiStatus.Internal
    class Holder {

        private static final SlimeInternalsBridge INSTANCE = ServiceLoader.load(
                SlimeInternalsBridge.class,
                SlimeInternalsBridge.class.getClassLoader()
        ).findFirst().orElseThrow(() -> new IllegalStateException("There is no Slime Internals Bridge service provider found!"));

    }

}
