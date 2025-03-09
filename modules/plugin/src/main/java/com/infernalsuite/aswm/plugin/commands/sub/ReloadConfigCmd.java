package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class ReloadConfigCmd extends SlimeCommand {

    public ReloadConfigCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swm|aswm|swp reload")
    @CommandDescription("Reloads the config files.")
    @Permission("swm.reload")
    public CompletableFuture<Void> reloadConfig(CommandSender sender) {
        return CompletableFuture.runAsync(() -> {
            try {
                ConfigManager.initialize();
            } catch (IOException ex) {
                log.error("Failed to load config files!", ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to reload the config file. Take a look at the server console for more information.").color(NamedTextColor.RED)
                ));
            }

            sender.sendMessage(COMMAND_PREFIX.append(
                    Component.text("Config reloaded.").color(NamedTextColor.GREEN)
            ));
        });
    }
}

