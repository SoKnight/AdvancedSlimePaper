package com.infernalsuite.aswm.api.exceptions;

/**
 * Exception thrown when a world has different
 * world version than currently used on server.
 */
public class MismatchedWorldVersionException extends SlimeException {

    public MismatchedWorldVersionException(int actual, int expected) {
        super("Target world has different version (%d) than expected (%d)".formatted(actual, expected));
    }

}
