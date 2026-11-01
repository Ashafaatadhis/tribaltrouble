package com.oddlabs.tt.window;

import com.oddlabs.tt.input.InputProvider;
import com.oddlabs.tt.input.LWJGL3InputProvider;
import com.oddlabs.tt.input.scripted.ScriptedInputProvider;
import org.jspecify.annotations.NonNull;

public final class DesktopWindowSystem implements WindowSystem {
    @Override
    public @NonNull Window createWindow() {
        return new LWJGL3Window();
    }

    @Override
    public @NonNull InputProvider<?> createInputProvider(@NonNull Window window) {
        if (!(window instanceof LWJGL3Window glfwWindow)) {
            throw new IllegalStateException("Window is not LWJGL3Window");
        }
        LWJGL3InputProvider glfwProvider = new LWJGL3InputProvider(glfwWindow);
        int port = Integer.getInteger("com.oddlabs.tt.input.port", 0);
        return port > 0 ? new ScriptedInputProvider(glfwProvider, glfwWindow, port) : glfwProvider;
    }
}
