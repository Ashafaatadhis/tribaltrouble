package org.lwjgl.glfw;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/** Placeholder for the cursor image; nothing reads it back. */
public final class GLFWImage {
    private GLFWImage() {
    }

    public static GLFWImage malloc(MemoryStack stack) {
        return new GLFWImage();
    }

    public GLFWImage set(int width, int height, ByteBuffer pixels) {
        return this;
    }
}
