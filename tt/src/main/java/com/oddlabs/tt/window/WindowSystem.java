package com.oddlabs.tt.window;

import com.oddlabs.tt.input.InputProvider;
import org.jspecify.annotations.NonNull;

/** Creates the platform's window and the input provider that feeds from it. */
public interface WindowSystem {
    @NonNull
    Window createWindow();

    @NonNull
    InputProvider<?> createInputProvider(@NonNull Window window);
}
