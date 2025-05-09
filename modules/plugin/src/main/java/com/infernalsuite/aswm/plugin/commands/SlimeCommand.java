package com.infernalsuite.aswm.plugin.commands;

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.MismatchedWorldVersionException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.plugin.SWPlugin;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.IOException;

public class SlimeCommand {

    public static final TextComponent COMMAND_PREFIX = LegacyComponentSerializer.legacySection().deserialize("§9§lSWP §7§l>> §r");

    protected final CommandManager commandManager;
    protected final SWPlugin plugin;
    protected final AdvancedSlimePaperAPI api;

    public SlimeCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
        this.plugin = commandManager.getPlugin();
        this.api = AdvancedSlimePaperAPI.get();
    }

    // This method is here so that we can easily change the behavior in the future
    protected SlimeWorld getWorldReadyForCloning(
            String name,
            SlimeLoader loader,
            SlimePropertyMap propertyMap
    ) throws CorruptedWorldException, NewerFormatException, UnknownWorldException, IOException, MismatchedWorldVersionException {
        return api.readWorld(loader, name, false, propertyMap);
    }

}
