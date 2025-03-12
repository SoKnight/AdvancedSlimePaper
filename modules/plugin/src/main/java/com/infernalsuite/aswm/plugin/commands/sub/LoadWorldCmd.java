package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.MismatchedWorldVersionException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.commands.parser.NamedWorldData;
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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class LoadWorldCmd extends SlimeCommand {

    public LoadWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swm|aswm|swp load <world>")
    @CommandDescription("Load a world")
    @Permission("swm.loadworld")
    public CompletableFuture<Void> onCommand(CommandSender sender, @Argument(value = "world") NamedWorldData worldData) {
        World world = Bukkit.getWorld(worldData.name());
        if (world != null)
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World '%s' is already loaded!".formatted(worldData.name())).color(NamedTextColor.RED)
            ));

        if (commandManager.getWorldsInUse().contains(worldData.name()))
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World '%s' is already being used on another command! Wait some time and try again.".formatted(worldData.name())).color(NamedTextColor.RED)
            ));

        commandManager.getWorldsInUse().add(worldData.name());

        sender.sendMessage(COMMAND_PREFIX.append(
                Component.text("Loading world ").color(NamedTextColor.GRAY)
                        .append(Component.text(worldData.name()).color(NamedTextColor.YELLOW))
                        .append(Component.text("...")).color(NamedTextColor.GRAY)
        ));

        // It's best to load the world async, and then just go back to the server thread and add it to the world list
        return CompletableFuture.runAsync(() -> {
            try {
                // ATTEMPT TO LOAD WORLD
                long start = System.currentTimeMillis();

                SlimeLoader loader = plugin.getLoaderManager().getLoader(worldData.worldData().getDataSource());
                if (loader == null)
                    throw new IllegalArgumentException("invalid data source '%s'".formatted(worldData.worldData().getDataSource()));

                SlimeWorld slimeWorld = api.readWorld(loader, worldData.name(), worldData.worldData().isReadOnly(), worldData.worldData().toPropertyMap());

                ExecutorUtil.runSyncAndWait(plugin, () -> {
                    try {
                        api.loadWorld(slimeWorld, true);
                    } catch (IllegalArgumentException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Failed to generate world '%s': %s".formatted(worldData.name(), ex.getMessage())).color(NamedTextColor.RED)
                        ));
                    }

                    sender.sendMessage(COMMAND_PREFIX.append(
                            Component.text("World ").color(NamedTextColor.GREEN)
                                    .append(Component.text(worldData.name()).color(NamedTextColor.YELLOW))
                                    .append(Component.text(" loaded and generated in " + (System.currentTimeMillis() - start) + "ms!").color(NamedTextColor.GREEN)
                            )
                    ));
                });
            } catch (MismatchedWorldVersionException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': world has unexpected data version %d (expected: %d).".formatted(worldData.name(), ex.getActual(), ex.getExpected())).color(NamedTextColor.RED)
                ));
            } catch (CorruptedWorldException ex) {
                log.error("Failed to load world '{}': world seems to be corrupted.", worldData.name(), ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': world seems to be corrupted.".formatted(worldData.name())).color(NamedTextColor.RED)
                ));
            } catch (NewerFormatException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': this world was serialized with a newer version of the Slime Format (%s) that SWM cannot understand.".formatted(worldData.name(), ex.getMessage())).color(NamedTextColor.RED)
                ));
            } catch (UnknownWorldException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': world could not be found (using data source '%s').".formatted(worldData.name(), worldData.worldData().getDataSource())).color(NamedTextColor.RED)
                ));
            } catch (IllegalArgumentException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': %s".formatted(worldData.name(), ex.getMessage())).color(NamedTextColor.RED)
                ));
            } catch (IOException ex) {
                log.error("Failed to load world '{}':", worldData.name(), ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s'. Take a look at the server console for more information.".formatted(worldData.name())).color(NamedTextColor.RED)
                ));
            } finally {
                commandManager.getWorldsInUse().remove(worldData.name());
            }
        });
    }

}

