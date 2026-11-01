package org.lwjgl.opengl;

import android.opengl.GLES20;

public final class GL {
    private static GLCapabilities capabilities;

    private GL() {
    }

    /** Reads the extension list of the context current on this thread. */
    public static GLCapabilities createCapabilities() {
        capabilities = new GLCapabilities(GLES20.glGetString(GLES20.GL_EXTENSIONS));
        return capabilities;
    }

    public static GLCapabilities getCapabilities() {
        if (capabilities == null) {
            throw new IllegalStateException("GL.createCapabilities() has not been called");
        }
        return capabilities;
    }
}
