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
public final class LoadTemplateWorldCmd extends SlimeCommand {

    public LoadTemplateWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swm|aswm|swp load-template <template-world> <world-name>")
    @CommandDescription("Creates a temporary world using another as a template. This world will never be stored.")
    @Permission("swm.loadworld.template")
    public CompletableFuture<Void> onCommand(
            CommandSender sender,
            @Argument(value = "template-world") NamedWorldData templateWorldData,
            @Argument(value = "world-name") String worldName
    ) {
        World world = Bukkit.getWorld(worldName);
        if (world != null)
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World '%s' is already loaded!".formatted(worldName)).color(NamedTextColor.RED)
            ));

        if (templateWorldData.name().equals(worldName))
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("The template world name cannot be the same as the cloned world one!").color(NamedTextColor.RED)
            ));

        if (commandManager.getWorldsInUse().contains(worldName))
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World '%s' is already being used on another command! Wait some time and try again.".formatted(worldName)).color(NamedTextColor.RED)
            ));

        commandManager.getWorldsInUse().add(worldName);
        sender.sendMessage(COMMAND_PREFIX.append(
                Component.text("Creating world ").color(NamedTextColor.GRAY)
                        .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                        .append(Component.text(" using ").color(NamedTextColor.GRAY))
                        .append(Component.text(templateWorldData.name()).color(NamedTextColor.YELLOW))
                        .append(Component.text(" as a template...").color(NamedTextColor.GRAY)
                )
        ));

        // It's best to load the world async, and then just go back to the server thread and add it to the world list
        return CompletableFuture.runAsync(() -> {
            try {
                long start = System.currentTimeMillis();

                SlimeLoader loader = plugin.getLoaderManager().getLoader(templateWorldData.worldData().getDataSource());
                if (loader == null)
                    throw new IllegalArgumentException("invalid data source '%s'".formatted(templateWorldData.worldData().getDataSource()));

                SlimeWorld templateWorld = getWorldReadyForCloning(templateWorldData.name(), loader, templateWorldData.worldData().toPropertyMap());
                SlimeWorld slimeWorld = templateWorld.clone(worldName);

                ExecutorUtil.runSyncAndWait(plugin, () -> {
                    try {
                        api.loadWorld(slimeWorld, true);
                    } catch (IllegalArgumentException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Failed to generate world '%s': %s.".formatted(worldName, ex.getMessage())).color(NamedTextColor.RED)
                        ));
                    }
                });

                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("World ").color(NamedTextColor.GREEN)
                                .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                                .append(Component.text(" loaded and generated in " + (System.currentTimeMillis() - start) + "ms!")).color(NamedTextColor.GREEN)
                ));
            } catch (MismatchedWorldVersionException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': world has unexpected data version %d (expected: %d).".formatted(templateWorldData.name(), ex.getActual(), ex.getExpected())).color(NamedTextColor.RED)
                ));
            } catch (CorruptedWorldException ex) {
                log.error("Failed to load world '{}': world seems to be corrupted.", templateWorldData.name(), ex);
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': world seems to be corrupted.".formatted(templateWorldData.name())).color(NamedTextColor.RED)
                ));
            } catch (NewerFormatException ex) {
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': this world was serialized with a newer version of the Slime Format (%s) that SWM cannot understand.".formatted(templateWorldData.name(), ex.getMessage())).color(NamedTextColor.RED)
                ));
            } catch (UnknownWorldException ex) {
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': world could not be found (using data source '%s').".formatted(templateWorldData.name(), templateWorldData.worldData().getDataSource())).color(NamedTextColor.RED)
                ));
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s': %s".formatted(templateWorldData.name(), ex.getMessage())).color(NamedTextColor.RED)
                ));
            } catch (IOException ex) {
                log.error("Failed to load world '{}':", templateWorldData.name(), ex);
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world '%s'. Take a look at the server console for more information.".formatted(templateWorldData.name())).color(NamedTextColor.RED)
                ));
            } finally {
                commandManager.getWorldsInUse().remove(worldName);
            }
        });
    }

}

