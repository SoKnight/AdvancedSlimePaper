package com.infernalsuite.aswm.plugin.loader;

import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.loaders.file.FileLoader;
import com.infernalsuite.aswm.loaders.redis.RedisLoader;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import com.infernalsuite.aswm.plugin.config.DataSourcesConfig;
import io.lettuce.core.RedisException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class LoaderManager {

    private final Map<String, SlimeLoader> loaders;

    public LoaderManager() {
        this.loaders = new HashMap<>();
        DataSourcesConfig config = ConfigManager.getDataSourcesConfig();

        // File loader
        DataSourcesConfig.FileConfig fileConfig = config.getFileConfig();
        if (fileConfig.isEnabled()) {
            try {
                registerLoader("file", new FileLoader(Paths.get(fileConfig.getPath())));
            } catch (IOException ex) {
                log.error("Failed to create file backed loader!", ex);
            }
        }

        // Redis loader
        DataSourcesConfig.RedisConfig redisConfig = config.getRedisConfig();
        if (redisConfig.isEnabled()){
            try {
                registerLoader("redis", new RedisLoader(redisConfig.getUri()));
            } catch (RedisException ex) {
                log.error("Failed to establish connection to the Redis server!", ex);
            }
        }
    }

    public void registerLoader(String dataSource, SlimeLoader loader) {
        if (loaders.containsKey(dataSource))
            throw new IllegalArgumentException("Data source '%s' already has a declared loader!".formatted(dataSource));

        loaders.put(dataSource, loader);
    }

    public SlimeLoader getLoader(String dataSource) {
        return loaders.get(dataSource);
    }

}
