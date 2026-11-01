package org.lwjgl.opengl;

public final class GLCapabilities {
    public final boolean OpenGL31;
    public final boolean OpenGL32;
    public final boolean GL_EXT_texture_filter_anisotropic;
    public final boolean GL_EXT_texture_compression_s3tc;

    GLCapabilities(int major, int minor, String extensions) {
        OpenGL31 = major > 3 || (major == 3 && minor >= 1);
        OpenGL32 = major > 3 || (major == 3 && minor >= 2);
        GL_EXT_texture_filter_anisotropic = extensions.contains("GL_EXT_texture_filter_anisotropic");
        GL_EXT_texture_compression_s3tc = extensions.contains("GL_EXT_texture_compression_s3tc");
    }
}
