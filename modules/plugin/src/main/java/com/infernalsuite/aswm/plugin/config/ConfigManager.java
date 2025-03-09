package com.infernalsuite.aswm.plugin.config;

import com.infernalsuite.aswm.plugin.SWPlugin;
import io.leangen.geantyref.TypeToken;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigManager {

    private static final Path PLUGIN_DIR = Paths.get("plugins", "SlimeWorldManager");
    private static final Path SOURCES_FILE = PLUGIN_DIR.resolve("data-sources.yml");
    private static final Path WORLDS_FILE = PLUGIN_DIR.resolve("worlds.yml");

    @Getter private static DataSourcesConfig dataSourcesConfig;
    @Getter private static WorldsConfig worldConfig;

    @Getter(AccessLevel.PACKAGE) private static YamlConfigurationLoader dataSourcesConfigLoader;
    @Getter(AccessLevel.PACKAGE) private static YamlConfigurationLoader worldConfigLoader;

    public static void initialize() throws IOException {
        copyDefaultConfigs();

        dataSourcesConfigLoader= createLoader(SOURCES_FILE);
        dataSourcesConfig = dataSourcesConfigLoader.load().get(TypeToken.get(DataSourcesConfig.class));
        dataSourcesConfig.save();

        worldConfigLoader = createLoader(WORLDS_FILE);
        worldConfig = worldConfigLoader.load().get(TypeToken.get(WorldsConfig.class));
        worldConfig.save();
    }

    private static void copyDefaultConfigs() throws IOException {
        if (!Files.isDirectory(PLUGIN_DIR))
            Files.createDirectories(PLUGIN_DIR);

        if (!Files.isRegularFile(SOURCES_FILE)) {
            Files.copy(SWPlugin.getInstance().getResource("data-sources.yml"), SOURCES_FILE);
        }

        if (!Files.isRegularFile(WORLDS_FILE)) {
            Files.copy(SWPlugin.getInstance().getResource("worlds.yml"), WORLDS_FILE);
        }
    }
    
    private static YamlConfigurationLoader createLoader(Path path) {
        return YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .headerMode(HeaderMode.PRESERVE)
                .build();
    }

}
