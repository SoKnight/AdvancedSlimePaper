package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.api.SlimeNMSBridge;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.commands.parser.NamedSlimeLoader;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class DSListCmd extends SlimeCommand {

    private static final int MAX_ITEMS_PER_PAGE = 5;

    public DSListCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swm|aswm|swp dslist <data-source> [page]")
    @CommandDescription("List all worlds inside a data source.")
    @Permission("swm.dslist")
    public CompletableFuture<Void> listWorlds(
            CommandSender sender,
            @Argument(value = "data-source") NamedSlimeLoader namedLoader,
            @Default("1") @Argument(value = "page") int page
    ) {
        SlimeLoader loader = namedLoader.slimeLoader();

        if (page < 1)
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("Page number must be greater than 0!").color(NamedTextColor.RED)
            ));

        return CompletableFuture.runAsync(() -> {
            List<String> worldList;

            try {
                //FIXME: This should utilize proper pagination and not fetch all worlds at once
                worldList = loader.listWorlds();
            } catch (IOException ex) {
                log.error("Failed to load world list:", ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world list. Take a look at the server console for more information.").color(NamedTextColor.RED)
                ));
            }

            if (worldList.isEmpty()) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("There are no worlds stored in data source '%s'.".formatted(namedLoader.name())).color(NamedTextColor.RED)
                ));
            }

            int offset = (page - 1) * MAX_ITEMS_PER_PAGE;
            double d = worldList.size() / (double) MAX_ITEMS_PER_PAGE;
            int maxPages = ((int) d) + ((d > (int) d) ? 1 : 0);

            if (offset >= worldList.size())
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("There %s only %d page%s!".formatted(maxPages == 1 ? "is" : "are", maxPages, maxPages == 1 ? "" : "s")).color(NamedTextColor.RED)
                ));

            worldList.sort(String::compareTo);
            sender.sendMessage(COMMAND_PREFIX.append(
                    Component.text("World list ").color(NamedTextColor.YELLOW)
                            .append(Component.text("[%d/%d]".formatted(page, maxPages)).color(NamedTextColor.YELLOW))
                            .append(Component.text(":").color(NamedTextColor.GRAY))
            ));

            for (int i = offset; (i - offset) < MAX_ITEMS_PER_PAGE && i < worldList.size(); i++) {
                String world = worldList.get(i);
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text(" - ").color(NamedTextColor.GRAY)
                                .append(isLoaded(loader, world)
                                        ? Component.text(world).color(NamedTextColor.GREEN)
                                        : Component.text(world).color(NamedTextColor.RED))
                ));
            }
        });
    }

    private boolean isLoaded(SlimeLoader loader, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null)
            return false;

        SlimeWorld slimeWorld = SlimeNMSBridge.instance().getInstance(world).getSlimeWorldMirror();
        if (slimeWorld == null)
            return false;

        return loader.equals(slimeWorld.getLoader());
    }

}
