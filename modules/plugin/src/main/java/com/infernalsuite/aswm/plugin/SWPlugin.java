package com.infernalsuite.aswm.plugin;

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI;
import com.infernalsuite.aswm.api.SlimeInternalsBridge;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.MismatchedWorldVersionException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import com.infernalsuite.aswm.plugin.config.WorldData;
import com.infernalsuite.aswm.plugin.config.WorldsConfig;
import com.infernalsuite.aswm.plugin.loader.LoaderManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class SWPlugin extends JavaPlugin {

    private static final AdvancedSlimePaperAPI API = AdvancedSlimePaperAPI.get();
    private static final SlimeInternalsBridge BRIDGE = SlimeInternalsBridge.get();
    private static SWPlugin INSTANCE;

    private final Map<String, SlimeWorld> worldsToLoad;
    @Getter private LoaderManager loaderManager;

    public SWPlugin() {
        INSTANCE = this;
        this.worldsToLoad = new HashMap<>();
    }

    @Override
    public void onLoad() {
        try {
            ConfigManager.initialize();
        } catch (NullPointerException | IOException ex) {
            getSLF4JLogger().error("Failed to load config files", ex);
            return;
        }

        this.loaderManager = new LoaderManager();
        BRIDGE.setLoaders(loaderManager.getLoaders());

        List<String> erroredWorlds = loadWorlds();

        // Default world override
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("server.properties"));

            String defaultWorldName = props.getProperty("level-name");
            if (erroredWorlds.contains(defaultWorldName)) {
                getSLF4JLogger().error("Shutting down server, as the default world could not be loaded.");
                Bukkit.getServer().shutdown();
            } else if (getServer().getAllowNether() && erroredWorlds.contains(defaultWorldName + "_nether")) {
                getSLF4JLogger().error("Shutting down server, as the default nether world could not be loaded.");
                Bukkit.getServer().shutdown();
            } else if (getServer().getAllowEnd() && erroredWorlds.contains(defaultWorldName + "_the_end")) {
                getSLF4JLogger().error("Shutting down server, as the default end world could not be loaded.");
                Bukkit.getServer().shutdown();
            }

            SlimeWorld defaultWorld = worldsToLoad.get(defaultWorldName);
            SlimeWorld netherWorld = getServer().getAllowNether() ? worldsToLoad.get(defaultWorldName + "_nether") : null;
            SlimeWorld endWorld = getServer().getAllowEnd() ? worldsToLoad.get(defaultWorldName + "_the_end") : null;

            BRIDGE.setDefaultWorlds(defaultWorld, netherWorld, endWorld);
        } catch (IOException ex) {
            getSLF4JLogger().error("Failed to retrieve default world name", ex);
        }
    }

    @Override
    public void onEnable() {
        CommandManager commandManager = new CommandManager(this);

        worldsToLoad.values().stream()
                .filter(slimeWorld -> Objects.isNull(Bukkit.getWorld(slimeWorld.getName())))
                .forEach(slimeWorld -> {
                    try {
                        API.loadWorld(slimeWorld, true);
                    } catch (RuntimeException exception) {
                        getSLF4JLogger().error("Failed to load world: {}", slimeWorld.getName(), exception);
                    }
                });

        worldsToLoad.clear(); // Don't unnecessarily hog up memory
    }

    @Override
    public void onDisable() {
        Bukkit.getWorlds().stream()
                .map(BRIDGE::getInstance)
                .filter(Objects::nonNull)
                .filter(world -> !world.isReadOnly())
                .forEach(world -> {
                    if (world instanceof SlimeWorld slimeWorld) {
                        try {
                            API.saveWorld(slimeWorld);
                        } catch (IOException ex) {
                            getSLF4JLogger().error("Failed to save world '{}'!", slimeWorld.getName(), ex);
                        }
                    }
                });
    }

    private List<String> loadWorlds() {
        List<String> erroredWorlds = new ArrayList<>();
        WorldsConfig config = ConfigManager.getWorldConfig();
        for (Map.Entry<String, WorldData> entry : config.getWorlds().entrySet()) {
            String worldName = entry.getKey();
            WorldData worldData = entry.getValue();
            if (worldData.isLoadOnStartup()) {
                try {
                    SlimeLoader loader = loaderManager.getLoader(worldData.getDataSource());
                    if (loader == null)
                        throw new IllegalArgumentException("invalid data source '%s'".formatted(worldData.getDataSource()));

                    SlimePropertyMap propertyMap = worldData.toPropertyMap();
                    SlimeWorld world = API.readWorld(loader, worldName, worldData.isReadOnly(), propertyMap);
                    worldsToLoad.put(worldName, world);
                } catch (IllegalArgumentException | UnknownWorldException | NewerFormatException |
                         CorruptedWorldException | IOException | MismatchedWorldVersionException ex) {
                    String message;
                    if (ex instanceof IllegalArgumentException) {
                        message = ex.getMessage();
                        //noinspection CallToPrintStackTrace
                        ex.printStackTrace();
                    } else if (ex instanceof UnknownWorldException) {
                        message = "world does not exist, are you sure you've set the correct data source?";
                    } else if (ex instanceof NewerFormatException) {
                        message = "world is serialized in a newer Slime Format version (" + ex.getMessage() + ") that this version of ASP does not understand.";
                    } else if (ex instanceof CorruptedWorldException) {
                        message = "world seems to be corrupted.";
                    } else {
                        message = "";
                        //noinspection CallToPrintStackTrace
                        ex.printStackTrace();
                    }

                    getSLF4JLogger().error("Failed to load world '{}'{}", worldName, message.isEmpty() ? "." : ": " + message);
                    erroredWorlds.add(worldName);
                }
            }
        }

        config.save();
        return erroredWorlds;
    }

    public static SWPlugin getInstance() {
        return INSTANCE;
    }

}
