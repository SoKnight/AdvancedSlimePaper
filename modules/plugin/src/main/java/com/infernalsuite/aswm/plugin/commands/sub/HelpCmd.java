package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.jetbrains.annotations.Nullable;

public final class HelpCmd extends SlimeCommand {

    private final MinecraftHelp<CommandSender> help;

    public HelpCmd(CommandManager commandManager, LegacyPaperCommandManager<CommandSender> cloudCommandManager) {
        super(commandManager);
        this.help = MinecraftHelp.createNative("/swm help", cloudCommandManager);
    }

    @Command("swm|aswm|swp help [query]")
    @CommandDescription("Displays the help message.")
    public void help(CommandSender sender, @Argument(value = "query") @Nullable String[] query) {
        String parsedQuery = query == null ? "" : String.join(" ", query);
        help.queryCommands(parsedQuery, sender);
    }

}
