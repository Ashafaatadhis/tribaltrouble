package org.lwjgl.opengl;

import android.opengl.GLES20;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GL {
    private static final Pattern ES_VERSION = Pattern.compile("OpenGL ES (\\d+)\\.(\\d+)");

    private static GLCapabilities capabilities;

    private GL() {
    }

    /** Reads the version and extension list of the context current on this thread. */
    public static GLCapabilities createCapabilities() {
        Matcher version = ES_VERSION.matcher(GLES20.glGetString(GLES20.GL_VERSION));
        if (!version.find()) {
            throw new IllegalStateException("Cannot read the OpenGL ES version");
        }
        capabilities = new GLCapabilities(Integer.parseInt(version.group(1)), Integer.parseInt(version.group(2)),
                GLES20.glGetString(GLES20.GL_EXTENSIONS));
        return capabilities;
    }

    public static GLCapabilities getCapabilities() {
        if (capabilities == null) {
            throw new IllegalStateException("GL.createCapabilities() has not been called");
        }
        return capabilities;
    }
}
