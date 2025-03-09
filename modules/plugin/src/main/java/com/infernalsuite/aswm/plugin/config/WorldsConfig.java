package com.infernalsuite.aswm.plugin.config;

import io.leangen.geantyref.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@ConfigSerializable
public final class WorldsConfig {

    @Setting("worlds")
    private final Map<String, WorldData> worlds;

    public WorldsConfig() {
        this.worlds = new HashMap<>();
    }

    public void save() {
        try {
            YamlConfigurationLoader loader = ConfigManager.getWorldConfigLoader();
            loader.save(loader.createNode().set(TypeToken.get(WorldsConfig.class), this));
        } catch (IOException ex) {
            log.error("Failed to save worlds config file!", ex);
        }
    }

}
