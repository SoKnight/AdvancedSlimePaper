package com.infernalsuite.aswm.plugin.config;

import io.leangen.geantyref.TypeToken;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;

@Slf4j
@Getter @Setter
@ConfigSerializable
public final class DataSourcesConfig {

    @Setting("file")
    private FileConfig fileConfig = new FileConfig();
    @Setting("redis")
    private RedisConfig redisConfig = new RedisConfig();

    @Getter
    @ConfigSerializable
    public static final class FileConfig {

        @Setting("enabled")
        private boolean enabled = false;
        @Setting("path")
        private String path = "slime_worlds";

    }

    @Getter
    @ConfigSerializable
    public static final class RedisConfig {

        @Setting("enabled")
        private boolean enabled = false;
        @Setting("uri")
        private String uri = "redis://127.0.0.1/";

    }

    public void save() {
        try {
            YamlConfigurationLoader loader = ConfigManager.getDataSourcesConfigLoader();
            loader.save(loader.createNode().set(TypeToken.get(DataSourcesConfig.class), this));
        } catch (IOException ex) {
            log.error("Failed to save datasources config file!", ex);
        }
    }

}
