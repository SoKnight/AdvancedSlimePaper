package com.infernalsuite.aswm.api.exceptions;

/**
 * Exception thrown when a world could not
 * be read from its data file.
 */
public class CorruptedWorldException extends SlimeException {

    public CorruptedWorldException(String world) {
        this(world, null);
    }

    public CorruptedWorldException(String world, Exception ex) {
        super("World '%s' seems to be corrupted!".formatted(world), ex);
    }

}
