package org.lwjgl.opengl;

public final class GLCapabilities {
    public final boolean GL_EXT_texture_filter_anisotropic;
    public final boolean GL_EXT_texture_compression_s3tc;

    GLCapabilities(String extensions) {
        GL_EXT_texture_filter_anisotropic = extensions.contains("GL_EXT_texture_filter_anisotropic");
        GL_EXT_texture_compression_s3tc = extensions.contains("GL_EXT_texture_compression_s3tc");
    }
}
