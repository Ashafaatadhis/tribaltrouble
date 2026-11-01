package com.oddlabs.tt.window;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Holds the platform's {@link WindowSystem}. It must be installed before {@code Renderer} is first used. */
public final class WindowSystems {
    private static volatile @Nullable WindowSystem installed;

    private WindowSystems() {
    }

    public static void install(@NonNull WindowSystem windowSystem) {
        installed = windowSystem;
    }

    public static @NonNull WindowSystem current() {
        WindowSystem windowSystem = installed;
        if (windowSystem == null) {
            throw new IllegalStateException("No window system installed");
        }
        return windowSystem;
    }
}
