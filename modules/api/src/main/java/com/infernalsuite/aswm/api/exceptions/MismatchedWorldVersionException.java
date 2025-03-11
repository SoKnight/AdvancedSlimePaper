package com.infernalsuite.aswm.api.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a world has different
 * world version than currently used on server.
 */
@Getter
public class MismatchedWorldVersionException extends SlimeException {

    private final int actual;
    private final int expected;

    public MismatchedWorldVersionException(int actual, int expected) {
        super("Target world has different version (%d) than expected (%d)".formatted(actual, expected));
        this.actual = actual;
        this.expected = expected;
    }

}
