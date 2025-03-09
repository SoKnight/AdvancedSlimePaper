package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;

import java.io.IOException;

@Slf4j
public final class SaveWorldCmd extends SlimeCommand {

    public SaveWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swm|aswm|swp save <world>")
    @CommandDescription("Saves a world.")
    @Permission("swm.saveworld")
    public void saveWorld(CommandSender sender, @Argument(value = "world") SlimeWorld slimeWorld) {
        try {
            api.saveWorld(slimeWorld);

            sender.sendMessage(COMMAND_PREFIX.append(
                    Component.text("World '%s' saved.".formatted(slimeWorld.getName())).color(NamedTextColor.GREEN)
            ));
        } catch (IOException e) {
            log.error("Failed to save world '{}'.", slimeWorld.getName(), e);
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("Failed to save world '%s'.".formatted(slimeWorld.getName())).color(NamedTextColor.RED)
            ));
        }
    }

}

