package com.infernalsuite.aswm.api.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.ServiceLoader;

// emulating original Kyori's Adventure API utility
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Services {

    public static <P> @NotNull P getService(@NotNull Class<P> type) {
        return findService(type).orElseThrow(() -> new IllegalArgumentException(
                "There are no service providers registered for '%s'!".formatted(type.getName())
        ));
    }

    public static <P> @NotNull Optional<P> findService(@NotNull Class<P> type) {
        return ServiceLoader.load(type, type.getClassLoader()).findFirst();
    }

}
