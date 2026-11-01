package com.oddlabs.tt.window;

import com.oddlabs.tt.input.AndroidInputProvider;
import com.oddlabs.tt.input.InputProvider;
import org.jspecify.annotations.NonNull;

/** Hands the engine the window and input provider the activity already created for its surface. */
public final class AndroidWindowSystem implements WindowSystem {
    private final @NonNull AndroidWindow window;
    private final @NonNull AndroidInputProvider inputProvider;

    public AndroidWindowSystem(@NonNull AndroidWindow window, @NonNull AndroidInputProvider inputProvider) {
        this.window = window;
        this.inputProvider = inputProvider;
    }

    @Override
    public @NonNull Window createWindow() {
        return window;
    }

    @Override
    public @NonNull InputProvider<?> createInputProvider(@NonNull Window window) {
        return inputProvider;
    }
}
