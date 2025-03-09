package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.api.SlimeNMSBridge;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.injection.RawArgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class WorldListCmd extends SlimeCommand {

    private static final int MAX_ITEMS_PER_PAGE = 5;

    public WorldListCmd(CommandManager commandManager) {
        super(commandManager);
    }

    //No way I'm going to use cloud args for this mess. What's even the point of this command? FIXME: Convert to cloud args
    @RawArgs
    @Command("swm|aswm|swp list [slime] [page]")
    @CommandDescription("List all worlds. To only list slime worlds, use the 'slime' argument.")
    @Permission("swm.worldlist")
    public void listWorlds(
            CommandSender sender, String[] args,
            // These two args are needed so that cloud doesn't complain
            @Argument(value = "slime") String rawSlime,
            @Argument(value = "page") String rawPage
    ) {
        args = Arrays.copyOfRange(args, 2, args.length); // Remove "swm|aswm list" from the args

        Map<String, Boolean> loadedWorlds = Bukkit.getWorlds().stream().collect(Collectors.toMap(
                World::getName,
                world -> SlimeNMSBridge.instance().getInstance(world) != null)
        );

        boolean onlySlime = args.length > 0 && args[0].equalsIgnoreCase("slime");
        if (onlySlime)
            loadedWorlds.entrySet().removeIf((entry) -> !entry.getValue());

        int page = parsePage(args);

        List<String> worldsList = new ArrayList<>(loadedWorlds.keySet());
        ConfigManager.getWorldConfig().getWorlds().keySet().stream()
                .filter((world) -> !worldsList.contains(world))
                .forEach(worldsList::add);

        if (worldsList.isEmpty())
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("There are no worlds configured.").color(NamedTextColor.RED)
            ));

        int offset = (page - 1) * MAX_ITEMS_PER_PAGE;
        double d = worldsList.size() / (double) MAX_ITEMS_PER_PAGE;
        int maxPages = ((int) d) + ((d > (int) d) ? 1 : 0);

        if (offset >= worldsList.size())
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("There %s only %d page%s!".formatted(maxPages == 1 ? "is" : "are", maxPages, maxPages == 1 ? "" : "s")).color(NamedTextColor.RED)
            ));

        worldsList.sort(String::compareTo);
        sender.sendMessage(COMMAND_PREFIX.append(
                Component.text("World list ").color(NamedTextColor.YELLOW)
                        .append(Component.text("[%d/%d]".formatted(page, maxPages)).color(NamedTextColor.YELLOW))
                        .append(Component.text(":").color(NamedTextColor.GRAY))
        ));

        for (int i = offset; (i - offset) < MAX_ITEMS_PER_PAGE && i < worldsList.size(); i++) {
            String world = worldsList.get(i);
            if (loadedWorlds.containsKey(world)) {
                TextComponent message = COMMAND_PREFIX.append(
                        Component.text(" - ").color(NamedTextColor.GRAY)
                                .append(Component.text(world).color(NamedTextColor.GREEN))
                );

                Boolean isSRF = loadedWorlds.get(world);
                if (isSRF == null || !isSRF) {
                    sender.sendMessage(message.append(Component.text(" ")
                            .append(Component.text("(not in SRF)").color(NamedTextColor.BLUE).decorate(TextDecoration.ITALIC))
                    ));
                } else {
                    sender.sendMessage(message);
                }
            } else {
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text(" - ").color(NamedTextColor.GRAY)
                                .append(Component.text(world).color(NamedTextColor.RED))
                ));
            }
        }
    }

    private static int parsePage(String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("slime"))) {
            return 1;
        } else {
            String pageString = args[args.length - 1];
            try {
                int page = Integer.parseInt(pageString);
                if (page >= 1)
                    return page;

                throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("'%s' is not a valid number.".formatted(pageString)).color(NamedTextColor.RED)
                ));
            }
        }
    }

}
