package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.api.exceptions.*;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.commands.parser.NamedSlimeLoader;
import com.infernalsuite.aswm.plugin.commands.parser.NamedWorldData;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import com.infernalsuite.aswm.plugin.config.WorldData;
import com.infernalsuite.aswm.plugin.config.WorldsConfig;
import com.infernalsuite.aswm.plugin.util.ExecutorUtil;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class CloneWorldCmd extends SlimeCommand {

    public CloneWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swm|aswm|swp clone-world <template-world> <world-name> [new-data-source]")
    @CommandDescription("Clones a world")
    @Permission("swm.cloneworld")
    public CompletableFuture<Void> cloneWorld(
            CommandSender sender,
            @Argument(value = "template-world") NamedWorldData templateWorld,
            @Argument(value = "world-name") String worldName,
            @Argument(value = "new-data-source") @Nullable NamedSlimeLoader slimeLoader
    ) {
        World world = Bukkit.getWorld(worldName);
        if (world != null)
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World '%s' is already loaded!".formatted(worldName))).color(NamedTextColor.RED)
            );

        WorldsConfig config = ConfigManager.getWorldConfig();
        WorldData worldData = templateWorld.worldData();

        if (templateWorld.name().equals(worldName))
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("The template world name cannot be the same as the cloned world one!")).color(NamedTextColor.RED)
            );

        if (commandManager.getWorldsInUse().contains(worldName))
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World '%s' is already being used on another command! Wait some time and try again.".formatted(worldName))).color(NamedTextColor.RED)
            );

        SlimeLoader initLoader = plugin.getLoaderManager().getLoader(worldData.getDataSource());
        SlimeLoader dataSource = slimeLoader == null ? initLoader : slimeLoader.slimeLoader();

        commandManager.getWorldsInUse().add(worldName);
        sender.sendMessage(COMMAND_PREFIX.append(
                Component.text("Creating world ").color(NamedTextColor.GRAY)
                        .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                        .append(Component.text(" using ").color(NamedTextColor.GRAY))
                        .append(Component.text(templateWorld.name()).color(NamedTextColor.YELLOW))
                        .append(Component.text(" as a template...").color(NamedTextColor.GRAY))
        ));

        // It's best to read the world async, and then just go back to the server thread and add it to the world list
        return CompletableFuture.runAsync(() -> {
            try {
                long start = System.currentTimeMillis();

                SlimeWorld slimeWorld = getWorldReadyForCloning(templateWorld.name(), initLoader, templateWorld.worldData().toPropertyMap());
                SlimeWorld finalSlimeWorld = slimeWorld.clone(worldName, dataSource);

                ExecutorUtil.runSyncAndWait(plugin, () -> {
                    try {
                        api.loadWorld(finalSlimeWorld, true);
                        config.getWorlds().put(worldName, worldData);
                    } catch (IllegalArgumentException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Failed to generate world " + worldName + ": " + ex.getMessage() + ".").color(NamedTextColor.RED)
                        ));
                    }

                    sender.sendMessage(COMMAND_PREFIX.append(
                            Component.text("World ").color(NamedTextColor.GREEN)
                                    .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                                    .append(Component.text(" loaded and generated in " + (System.currentTimeMillis() - start) + "ms!").color(NamedTextColor.GREEN))
                    ));
                });
                config.save();
            } catch (MismatchedWorldVersionException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': world has unexpected data version %d (expected: %d).".formatted(templateWorld.name(), ex.getActual(), ex.getExpected())).color(NamedTextColor.RED)
                ));
            } catch (WorldAlreadyExistsException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("There is already a world called '%s' stored in '%s'.".formatted(worldName, dataSource)).color(NamedTextColor.RED)
                ));
            } catch (CorruptedWorldException ex) {
                log.error("Failed to load world '{}': world seems to be corrupted.", templateWorld.name(), ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': world seems to be corrupted.".formatted(templateWorld.name())).color(NamedTextColor.RED)
                ));
            } catch (NewerFormatException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': this world was serialized with a newer version of the Slime Format (%s) that SWM cannot understand.".formatted(templateWorld.name(), ex.getMessage())).color(NamedTextColor.RED)
                ));
            } catch (UnknownWorldException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': world could not be found (using data source '%s').".formatted(templateWorld.name(), worldData.getDataSource())).color(NamedTextColor.RED)
                ));
            } catch (IllegalArgumentException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': %s".formatted(templateWorld.name(), ex.getMessage())).color(NamedTextColor.RED)
                ));
            } catch (IOException ex) {
                log.error("Failed to load world '{}'.", templateWorld.name(), ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s'. Take a look at the server console for more information.".formatted(templateWorld.name())).color(NamedTextColor.RED)
                ));
            } finally {
                commandManager.getWorldsInUse().remove(worldName);
            }
        });
    }

}

