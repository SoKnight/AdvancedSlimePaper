package com.infernalsuite.aswm.plugin.commands.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;

@Getter
@AllArgsConstructor
public final class MessageCommandException extends RuntimeException {

    private final Component component;

}
