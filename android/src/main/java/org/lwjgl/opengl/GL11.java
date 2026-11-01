package org.lwjgl.opengl;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.opengl.GLES32;
import com.oddlabs.tt.render.shader.ShaderDialect;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Android implementation of the LWJGL GL entry points the client uses, over GLES 3.2. All functions and
 * constants live here; {@code GL12} to {@code GL41} extend it so call sites such as
 * {@code GL30.GL_FRAMEBUFFER} resolve unchanged. Desktop-only calls are no-ops or mapped as noted.
 */
public class GL11 {
    public static final int GL_FALSE = GLES20.GL_FALSE;
    public static final int GL_TRUE = GLES20.GL_TRUE;
    public static final int GL_NONE = GLES20.GL_NONE;
    public static final int GL_ZERO = GLES20.GL_ZERO;
    public static final int GL_ONE = GLES20.GL_ONE;

    public static final int GL_POINTS = GLES20.GL_POINTS;
    public static final int GL_LINES = GLES20.GL_LINES;
    public static final int GL_LINE_STRIP = GLES20.GL_LINE_STRIP;
    public static final int GL_TRIANGLES = GLES20.GL_TRIANGLES;
    public static final int GL_TRIANGLE_STRIP = GLES20.GL_TRIANGLE_STRIP;
    public static final int GL_TRIANGLE_FAN = GLES20.GL_TRIANGLE_FAN;
    /** Not drawable in ES; only {@code ShortVBO} uses it, to count indices. */
    public static final int GL_QUADS = 0x0007;
    public static final int GL_QUAD_STRIP = 0x0008;

    public static final int GL_COLOR_BUFFER_BIT = GLES20.GL_COLOR_BUFFER_BIT;
    public static final int GL_DEPTH_BUFFER_BIT = GLES20.GL_DEPTH_BUFFER_BIT;

    public static final int GL_BLEND = GLES20.GL_BLEND;
    public static final int GL_CULL_FACE = GLES20.GL_CULL_FACE;
    public static final int GL_DEPTH_TEST = GLES20.GL_DEPTH_TEST;
    public static final int GL_SCISSOR_TEST = GLES20.GL_SCISSOR_TEST;
    public static final int GL_POLYGON_OFFSET_FILL = GLES20.GL_POLYGON_OFFSET_FILL;
    public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = GLES20.GL_SAMPLE_ALPHA_TO_COVERAGE;
    /** Not a valid capability in ES; {@link #glEnable} and {@link #glDisable} ignore it. */
    public static final int GL_MULTISAMPLE = 0x809D;

    public static final int GL_SRC_ALPHA = GLES20.GL_SRC_ALPHA;
    public static final int GL_ONE_MINUS_SRC_ALPHA = GLES20.GL_ONE_MINUS_SRC_ALPHA;
    public static final int GL_FRONT = GLES20.GL_FRONT;
    public static final int GL_BACK = GLES20.GL_BACK;
    public static final int GL_FRONT_AND_BACK = GLES20.GL_FRONT_AND_BACK;
    /** Desktop default-framebuffer buffer name; mapped to {@code GL_BACK} where ES needs it. */
    public static final int GL_BACK_LEFT = 0x0402;
    public static final int GL_CCW = GLES20.GL_CCW;
    public static final int GL_FILL = 0x1B02;
    public static final int GL_LINE = 0x1B01;
    public static final int GL_EQUAL = GLES20.GL_EQUAL;
    public static final int GL_LEQUAL = GLES20.GL_LEQUAL;
    public static final int GL_DEPTH_FUNC = GLES20.GL_DEPTH_FUNC;

    public static final int GL_NO_ERROR = GLES20.GL_NO_ERROR;
    public static final int GL_INVALID_ENUM = GLES20.GL_INVALID_ENUM;
    public static final int GL_INVALID_VALUE = GLES20.GL_INVALID_VALUE;
    public static final int GL_INVALID_OPERATION = GLES20.GL_INVALID_OPERATION;
    public static final int GL_OUT_OF_MEMORY = GLES20.GL_OUT_OF_MEMORY;
    public static final int GL_STACK_OVERFLOW = 0x0503;
    public static final int GL_STACK_UNDERFLOW = 0x0504;

    public static final int GL_VENDOR = GLES20.GL_VENDOR;
    public static final int GL_RENDERER = GLES20.GL_RENDERER;
    public static final int GL_VERSION = GLES20.GL_VERSION;
    public static final int GL_VIEWPORT = GLES20.GL_VIEWPORT;
    public static final int GL_SCISSOR_BOX = GLES20.GL_SCISSOR_BOX;
    public static final int GL_MAX_TEXTURE_SIZE = GLES20.GL_MAX_TEXTURE_SIZE;
    public static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS;

    public static final int GL_UNPACK_ALIGNMENT = GLES20.GL_UNPACK_ALIGNMENT;
    public static final int GL_PACK_ALIGNMENT = GLES20.GL_PACK_ALIGNMENT;
    public static final int GL_UNPACK_ROW_LENGTH = GLES30.GL_UNPACK_ROW_LENGTH;
    public static final int GL_UNPACK_SKIP_ROWS = GLES30.GL_UNPACK_SKIP_ROWS;
    public static final int GL_UNPACK_SKIP_PIXELS = GLES30.GL_UNPACK_SKIP_PIXELS;
    public static final int GL_PACK_ROW_LENGTH = GLES30.GL_PACK_ROW_LENGTH;
    public static final int GL_PACK_SKIP_ROWS = GLES30.GL_PACK_SKIP_ROWS;
    public static final int GL_PACK_SKIP_PIXELS = GLES30.GL_PACK_SKIP_PIXELS;
    /** Byte swapping does not exist in ES; {@link #glPixelStorei} ignores these. */
    public static final int GL_UNPACK_SWAP_BYTES = 0x0CF0;
    public static final int GL_PACK_SWAP_BYTES = 0x0D00;

    public static final int GL_TEXTURE_2D = GLES20.GL_TEXTURE_2D;
    public static final int GL_TEXTURE_MAG_FILTER = GLES20.GL_TEXTURE_MAG_FILTER;
    public static final int GL_TEXTURE_MIN_FILTER = GLES20.GL_TEXTURE_MIN_FILTER;
    public static final int GL_TEXTURE_WRAP_S = GLES20.GL_TEXTURE_WRAP_S;
    public static final int GL_TEXTURE_WRAP_T = GLES20.GL_TEXTURE_WRAP_T;
    public static final int GL_TEXTURE_MAX_LEVEL = GLES30.GL_TEXTURE_MAX_LEVEL;
    public static final int GL_TEXTURE_COMPARE_MODE = GLES30.GL_TEXTURE_COMPARE_MODE;
    public static final int GL_TEXTURE_COMPARE_FUNC = GLES30.GL_TEXTURE_COMPARE_FUNC;
    public static final int GL_COMPARE_REF_TO_TEXTURE = GLES30.GL_COMPARE_REF_TO_TEXTURE;
    public static final int GL_TEXTURE_BORDER_COLOR = GLES32.GL_TEXTURE_BORDER_COLOR;
    public static final int GL_TEXTURE_WIDTH = 0x1000;
    public static final int GL_TEXTURE_HEIGHT = 0x1001;
    public static final int GL_NEAREST = GLES20.GL_NEAREST;
    public static final int GL_LINEAR = GLES20.GL_LINEAR;
    public static final int GL_NEAREST_MIPMAP_LINEAR = GLES20.GL_NEAREST_MIPMAP_LINEAR;
    public static final int GL_LINEAR_MIPMAP_LINEAR = GLES20.GL_LINEAR_MIPMAP_LINEAR;
    public static final int GL_REPEAT = GLES20.GL_REPEAT;
    public static final int GL_CLAMP_TO_EDGE = GLES20.GL_CLAMP_TO_EDGE;
    public static final int GL_TEXTURE0 = GLES20.GL_TEXTURE0;
    public static final int GL_TEXTURE1 = GLES20.GL_TEXTURE1;
    public static final int GL_TEXTURE2 = GLES20.GL_TEXTURE2;
    public static final int GL_TEXTURE3 = GLES20.GL_TEXTURE3;
    public static final int GL_TEXTURE4 = GLES20.GL_TEXTURE4;

    public static final int GL_UNSIGNED_BYTE = GLES20.GL_UNSIGNED_BYTE;
    public static final int GL_UNSIGNED_SHORT = GLES20.GL_UNSIGNED_SHORT;
    public static final int GL_UNSIGNED_INT = GLES20.GL_UNSIGNED_INT;
    public static final int GL_FLOAT = GLES20.GL_FLOAT;
    public static final int GL_HALF_FLOAT = GLES30.GL_HALF_FLOAT;
    public static final int GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367;

    public static final int GL_ALPHA = GLES20.GL_ALPHA;
    public static final int GL_ALPHA8 = 0x803C;
    public static final int GL_LUMINANCE = GLES20.GL_LUMINANCE;
    public static final int GL_RED = GLES30.GL_RED;
    public static final int GL_R8 = GLES30.GL_R8;
    public static final int GL_R32F = GLES30.GL_R32F;
    public static final int GL_RGB = GLES20.GL_RGB;
    public static final int GL_RGB8 = GLES30.GL_RGB8;
    public static final int GL_RGB32F = GLES30.GL_RGB32F;
    public static final int GL_RGBA = GLES20.GL_RGBA;
    public static final int GL_RGBA8 = GLES30.GL_RGBA8;
    public static final int GL_RGBA16F = GLES30.GL_RGBA16F;
    /** Same value as {@code GL_BGRA_EXT}; only usable where the driver exposes that extension. */
    public static final int GL_BGRA = 0x80E1;
    public static final int GL_DEPTH_COMPONENT = GLES20.GL_DEPTH_COMPONENT;
    public static final int GL_DEPTH_COMPONENT24 = GLES30.GL_DEPTH_COMPONENT24;
    public static final int GL_DEPTH_COMPONENT32F = GLES30.GL_DEPTH_COMPONENT32F;
    public static final int GL_DEPTH = GLES30.GL_DEPTH;
    public static final int GL_STENCIL = GLES30.GL_STENCIL;

    /** Generic compressed formats do not exist in ES; kept so format switches still compile. */
    public static final int GL_COMPRESSED_ALPHA = 0x84E9;
    public static final int GL_COMPRESSED_LUMINANCE = 0x84EA;
    public static final int GL_COMPRESSED_RGB = 0x84ED;
    public static final int GL_COMPRESSED_RGBA = 0x84EE;
    public static final int GL_TEXTURE_COMPRESSED = 0x86A1;
    public static final int GL_TEXTURE_COMPRESSED_IMAGE_SIZE = 0x86A0;

    public static final int GL_ARRAY_BUFFER = GLES20.GL_ARRAY_BUFFER;
    public static final int GL_ELEMENT_ARRAY_BUFFER = GLES20.GL_ELEMENT_ARRAY_BUFFER;
    public static final int GL_UNIFORM_BUFFER = GLES30.GL_UNIFORM_BUFFER;
    public static final int GL_TEXTURE_BUFFER = GLES32.GL_TEXTURE_BUFFER;
    public static final int GL_STREAM_DRAW = GLES20.GL_STREAM_DRAW;
    public static final int GL_STATIC_DRAW = GLES20.GL_STATIC_DRAW;
    public static final int GL_DYNAMIC_DRAW = GLES20.GL_DYNAMIC_DRAW;

    public static final int GL_VERTEX_SHADER = GLES20.GL_VERTEX_SHADER;
    public static final int GL_FRAGMENT_SHADER = GLES20.GL_FRAGMENT_SHADER;
    public static final int GL_GEOMETRY_SHADER = GLES32.GL_GEOMETRY_SHADER;
    public static final int GL_COMPILE_STATUS = GLES20.GL_COMPILE_STATUS;
    public static final int GL_LINK_STATUS = GLES20.GL_LINK_STATUS;

    public static final int GL_FRAMEBUFFER = GLES20.GL_FRAMEBUFFER;
    public static final int GL_FRAMEBUFFER_COMPLETE = GLES20.GL_FRAMEBUFFER_COMPLETE;
    public static final int GL_COLOR_ATTACHMENT0 = GLES20.GL_COLOR_ATTACHMENT0;
    public static final int GL_COLOR_ATTACHMENT1 = GLES30.GL_COLOR_ATTACHMENT1;
    public static final int GL_DEPTH_ATTACHMENT = GLES20.GL_DEPTH_ATTACHMENT;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE = GLES30.GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE = GLES30.GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE = GLES30.GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE = GLES30.GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE = GLES30.GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE = GLES30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE;

    protected GL11() {
    }

    private static int sizeOf(Buffer data) {
        return switch (data) {
            case ByteBuffer b -> b.remaining();
            case ShortBuffer s -> s.remaining() * Short.BYTES;
            case IntBuffer i -> i.remaining() * Integer.BYTES;
            case FloatBuffer f -> f.remaining() * Float.BYTES;
            case LongBuffer l -> l.remaining() * Long.BYTES;
            case DoubleBuffer d -> d.remaining() * Double.BYTES;
            default -> throw new IllegalArgumentException("Unsupported buffer " + data.getClass());
        };
    }

    // --- state ---

    public static void glEnable(int cap) {
        if (cap != GL_MULTISAMPLE) {
            GLES20.glEnable(cap);
        }
    }

    public static void glDisable(int cap) {
        if (cap != GL_MULTISAMPLE) {
            GLES20.glDisable(cap);
        }
    }

    public static boolean glIsEnabled(int cap) {
        return GLES20.glIsEnabled(cap);
    }

    public static void glBlendFunc(int sfactor, int dfactor) {
        GLES20.glBlendFunc(sfactor, dfactor);
    }

    public static void glClear(int mask) {
        GLES20.glClear(mask);
    }

    public static void glClearColor(float r, float g, float b, float a) {
        GLES20.glClearColor(r, g, b, a);
    }

    public static void glClearDepth(double depth) {
        GLES20.glClearDepthf((float) depth);
    }

    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) {
        GLES20.glColorMask(r, g, b, a);
    }

    public static void glCullFace(int mode) {
        GLES20.glCullFace(mode);
    }

    public static void glDepthFunc(int func) {
        GLES20.glDepthFunc(func);
    }

    public static void glDepthMask(boolean flag) {
        GLES20.glDepthMask(flag);
    }

    public static void glFrontFace(int mode) {
        GLES20.glFrontFace(mode);
    }

    public static void glPolygonOffset(float factor, float units) {
        GLES20.glPolygonOffset(factor, units);
    }

    /** No-op: ES has no polygon mode, so the debug wireframe view does nothing. */
    public static void glPolygonMode(int face, int mode) {
    }

    public static void glScissor(int x, int y, int width, int height) {
        GLES20.glScissor(x, y, width, height);
    }

    public static void glViewport(int x, int y, int width, int height) {
        GLES20.glViewport(x, y, width, height);
    }

    public static int glGetError() {
        return GLES20.glGetError();
    }

    public static String glGetString(int name) {
        return GLES20.glGetString(name);
    }

    public static int glGetInteger(int pname) {
        int[] value = new int[1];
        GLES20.glGetIntegerv(pname, value, 0);
        return value[0];
    }

    public static void glGetIntegerv(int pname, IntBuffer params) {
        GLES20.glGetIntegerv(pname, params);
    }

    public static float glGetFloat(int pname) {
        float[] value = new float[1];
        GLES20.glGetFloatv(pname, value, 0);
        return value[0];
    }

    public static void glPixelStorei(int pname, int param) {
        if (pname != GL_UNPACK_SWAP_BYTES && pname != GL_PACK_SWAP_BYTES) {
            GLES20.glPixelStorei(pname, param);
        }
    }

    // --- draw ---

    public static void glDrawArrays(int mode, int first, int count) {
        GLES20.glDrawArrays(mode, first, count);
    }

    /** Draws from the bound element array buffer, starting {@code offset} bytes in. */
    public static void glDrawElements(int mode, int count, int type, long offset) {
        GLES30.glDrawElements(mode, count, type, (int) offset);
    }

    /** Maps the desktop draw buffer names onto the ES ones. */
    public static void glDrawBuffer(int buf) {
        GLES30.glDrawBuffers(1, new int[]{buf == GL_BACK_LEFT ? GL_BACK : buf}, 0);
    }

    public static void glReadBuffer(int src) {
        GLES30.glReadBuffer(src == GL_BACK_LEFT ? GL_BACK : src);
    }

    public static void glDrawBuffers(int buf) {
        glDrawBuffer(buf);
    }

    public static void glDrawBuffers(int[] bufs) {
        GLES30.glDrawBuffers(bufs.length, bufs, 0);
    }

    public static void glDrawBuffers(IntBuffer bufs) {
        GLES30.glDrawBuffers(bufs.remaining(), bufs);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer pixels) {
        GLES20.glReadPixels(x, y, width, height, format, type, pixels);
    }

    // --- textures ---

    public static void glActiveTexture(int texture) {
        GLES20.glActiveTexture(texture);
    }

    /** Texture buffers are emulated by 2D textures, so binding one binds that. */
    public static void glBindTexture(int target, int texture) {
        GLES20.glBindTexture(target == GL_TEXTURE_BUFFER ? GL_TEXTURE_2D : target, texture);
    }

    public static int glGenTextures() {
        int[] id = new int[1];
        GLES20.glGenTextures(1, id, 0);
        return id[0];
    }

    public static void glGenTextures(IntBuffer textures) {
        GLES20.glGenTextures(textures.remaining(), textures);
    }

    public static void glDeleteTextures(int texture) {
        GLES20.glDeleteTextures(1, new int[]{texture}, 0);
    }

    public static void glDeleteTextures(IntBuffer textures) {
        GLES20.glDeleteTextures(textures.remaining(), textures);
    }

    public static void glTexParameteri(int target, int pname, int param) {
        GLES20.glTexParameteri(target, pname, param);
    }

    public static void glTexParameterf(int target, int pname, float param) {
        GLES20.glTexParameterf(target, pname, param);
    }

    /** Ignores the border colour: it needs ES 3.2 and the game never clamps to the border. */
    public static void glTexParameterfv(int target, int pname, FloatBuffer params) {
        if (pname != GL_TEXTURE_BORDER_COLOR) {
            GLES20.glTexParameterfv(target, pname, params);
        }
    }

    /**
     * Maps desktop-only combinations onto ones ES accepts: unsized "red" and generic compressed internal formats become
     * sized ones, BGRA bytes are swizzled to RGBA, and RGBA bytes are narrowed when the texture is RGB.
     */
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
            int format, int type, Buffer pixels) {
        int internal = esInternalFormat(internalformat);
        int uploadFormat = format;
        int uploadType = type;
        Buffer data = pixels;
        if (internal == GL_R8 && (format == GL_LUMINANCE || format == GL_ALPHA)) {
            uploadFormat = GL_RED;
        }
        if (pixels == null) {
            uploadFormat = allocationFormat(internal, format);
            uploadType = allocationType(internal, type);
        }
        if (pixels instanceof ByteBuffer bytes && type == GL_UNSIGNED_BYTE) {
            boolean narrow = (internal == GL_RGB || internal == GL_RGB8) && (format == GL_RGBA || format == GL_BGRA);
            if (format == GL_BGRA || narrow) {
                data = repack(bytes, width * height, format == GL_BGRA, narrow ? 3 : 4);
                uploadFormat = narrow ? GL_RGB : GL_RGBA;
            }
        }
        GLES20.glTexImage2D(target, level, internal, width, height, border, uploadFormat, uploadType, data);
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,
            int format, int type, Buffer pixels) {
        int uploadFormat = format;
        Buffer data = pixels;
        if (format == GL_BGRA && pixels instanceof ByteBuffer bytes && type == GL_UNSIGNED_BYTE) {
            data = repack(bytes, width * height, true, 4);
            uploadFormat = GL_RGBA;
        }
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, uploadFormat, type, data);
    }

    /** Empty storage still has to name a format and type that ES accepts for the internal format. */
    private static int allocationFormat(int internal, int format) {
        return switch (internal) {
            case GL_R8, GL_R32F -> GL_RED;
            case GL_RGB, GL_RGB8, GL_RGB32F -> GL_RGB;
            case GL_RGBA, GL_RGBA8, GL_RGBA16F -> GL_RGBA;
            case GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT32F -> GL_DEPTH_COMPONENT;
            default -> format;
        };
    }

    private static int allocationType(int internal, int type) {
        return switch (internal) {
            case GL_R8, GL_RGB, GL_RGB8, GL_RGBA, GL_RGBA8 -> GL_UNSIGNED_BYTE;
            case GL_R32F, GL_RGB32F, GL_DEPTH_COMPONENT32F -> GL_FLOAT;
            case GL_RGBA16F -> GL_HALF_FLOAT;
            case GL_DEPTH_COMPONENT24 -> GL_UNSIGNED_INT;
            default -> type;
        };
    }

    private static int esInternalFormat(int internalformat) {
        return switch (internalformat) {
            case GL_RED, GL_COMPRESSED_LUMINANCE, GL_COMPRESSED_ALPHA -> GL_R8;
            case GL_COMPRESSED_RGB -> GL_RGB8;
            case GL_COMPRESSED_RGBA -> GL_RGBA8;
            default -> internalformat;
        };
    }

    /** Copies four-byte pixels into a direct buffer, optionally swapping red and blue and dropping alpha. */
    private static ByteBuffer repack(ByteBuffer source, int pixels, boolean swapRedBlue, int outBytes) {
        ByteBuffer in = source.duplicate();
        ByteBuffer out = ByteBuffer.allocateDirect(pixels * outBytes);
        for (int i = 0; i < pixels; i++) {
            byte c0 = in.get();
            byte c1 = in.get();
            byte c2 = in.get();
            byte alpha = in.get();
            out.put(swapRedBlue ? c2 : c0).put(c1).put(swapRedBlue ? c0 : c2);
            if (outBytes == 4) {
                out.put(alpha);
            }
        }
        out.flip();
        return out;
    }

    /** Decodes S3TC data to RGBA on the CPU when the GPU lacks the extension, as most mobile GPUs do. */
    public static void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height,
            int border, ByteBuffer data) {
        if (isS3tc(internalformat) && !GL.getCapabilities().GL_EXT_texture_compression_s3tc) {
            boolean dxt5 = internalformat == EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
            boolean dxt1Alpha = internalformat == EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            S3tcDecoder.Packing packing = dxt5
                    ? S3tcDecoder.Packing.RGBA4444
                    : dxt1Alpha ? S3tcDecoder.Packing.RGBA5551 : S3tcDecoder.Packing.RGB565;
            int[] alignment = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_UNPACK_ALIGNMENT, alignment, 0);
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 2);
            switch (packing) {
                case RGB565 -> GLES20.glTexImage2D(target, level, GLES20.GL_RGB565, width, height, border, GL_RGB,
                        GLES20.GL_UNSIGNED_SHORT_5_6_5, S3tcDecoder.decode(data, width, height, dxt5, dxt1Alpha, packing));
                case RGBA5551 -> GLES20.glTexImage2D(target, level, GLES20.GL_RGB5_A1, width, height, border, GL_RGBA,
                        GLES20.GL_UNSIGNED_SHORT_5_5_5_1, S3tcDecoder.decode(data, width, height, dxt5, dxt1Alpha, packing));
                default -> GLES20.glTexImage2D(target, level, GLES20.GL_RGBA4, width, height, border, GL_RGBA,
                        GLES20.GL_UNSIGNED_SHORT_4_4_4_4, S3tcDecoder.decode(data, width, height, dxt5, dxt1Alpha, packing));
            }
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, alignment[0]);
            return;
        }
        GLES20.glCompressedTexImage2D(target, level, internalformat, width, height, border, data.remaining(), data);
    }

    private static boolean isS3tc(int internalformat) {
        return internalformat == EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT
                || internalformat == EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT
                || internalformat == EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
    }

    /** Not in ES. Read the texture back through a framebuffer and {@code glReadPixels} instead. */
    public static void glGetTexImage(int target, int level, int format, int type, Buffer pixels) {
        throw new UnsupportedOperationException("glGetTexImage is not available in OpenGL ES");
    }

    /** ES cannot report compression state or size, so those queries answer 0 and callers fall back to math. */
    public static void glGetTexLevelParameteriv(int target, int level, int pname, IntBuffer params) {
        if (pname == GL_TEXTURE_COMPRESSED || pname == GL_TEXTURE_COMPRESSED_IMAGE_SIZE) {
            params.put(params.position(), 0);
        } else {
            GLES31.glGetTexLevelParameteriv(target, level, pname, params);
        }
    }

    public static void glGenerateMipmap(int target) {
        GLES20.glGenerateMipmap(target);
    }

    // --- buffers ---

    public static void glBindBuffer(int target, int buffer) {
        GLES20.glBindBuffer(target, buffer);
    }

    public static int glGenBuffers() {
        int[] id = new int[1];
        GLES20.glGenBuffers(1, id, 0);
        return id[0];
    }

    public static void glGenBuffers(IntBuffer buffers) {
        GLES20.glGenBuffers(buffers.remaining(), buffers);
    }

    public static void glDeleteBuffers(int buffer) {
        GLES20.glDeleteBuffers(1, new int[]{buffer}, 0);
    }

    public static void glDeleteBuffers(IntBuffer buffers) {
        GLES20.glDeleteBuffers(buffers.remaining(), buffers);
    }

    public static void glBufferData(int target, long size, int usage) {
        GLES20.glBufferData(target, (int) size, null, usage);
    }

    public static void glBufferData(int target, Buffer data, int usage) {
        GLES20.glBufferData(target, sizeOf(data), data, usage);
    }

    public static void glBufferSubData(int target, long offset, Buffer data) {
        GLES20.glBufferSubData(target, (int) offset, sizeOf(data), data);
    }

    public static void glBindBufferBase(int target, int index, int buffer) {
        GLES30.glBindBufferBase(target, index, buffer);
    }

    /** Needs the ES 3.2 texture buffer; {@code GL_RGB32F} is not a valid buffer format there, see the port notes. */
    public static void glTexBuffer(int target, int internalformat, int buffer) {
        emulateTexBuffer(buffer);
    }

    /**
     * Copies the RGB32F contents of the buffer into the bound 2D texture, laid out in rows of
     * {@code 1 << ShaderDialect.TEXTURE_BUFFER_ROW_BITS} texels, which the converted shaders index into. The copy is a
     * snapshot, so later changes to the buffer are not seen.
     */
    private static void emulateTexBuffer(int buffer) {
        int[] size = new int[1];
        GLES20.glBindBuffer(GLES30.GL_COPY_READ_BUFFER, buffer);
        GLES20.glGetBufferParameteriv(GLES30.GL_COPY_READ_BUFFER, GLES20.GL_BUFFER_SIZE, size, 0);
        ByteBuffer mapped = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_COPY_READ_BUFFER, 0, size[0],
                GLES30.GL_MAP_READ_BIT);
        int rowTexels = 1 << ShaderDialect.TEXTURE_BUFFER_ROW_BITS;
        int texels = size[0] / (3 * Float.BYTES);
        int rows = (texels + rowTexels - 1) / rowTexels;
        ByteBuffer padded = ByteBuffer.allocateDirect(rows * rowTexels * 3 * Float.BYTES).order(ByteOrder.nativeOrder());
        padded.put(mapped);
        padded.flip();
        GLES30.glUnmapBuffer(GLES30.GL_COPY_READ_BUFFER);
        GLES20.glBindBuffer(GLES30.GL_COPY_READ_BUFFER, 0);

        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GLES20.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, rowTexels, rows, 0, GL_RGB, GL_FLOAT, padded);
    }

    // --- vertex arrays and attributes ---

    public static int glGenVertexArrays() {
        int[] id = new int[1];
        GLES30.glGenVertexArrays(1, id, 0);
        return id[0];
    }

    public static void glBindVertexArray(int array) {
        GLES30.glBindVertexArray(array);
    }

    public static void glDeleteVertexArrays(int array) {
        GLES30.glDeleteVertexArrays(1, new int[]{array}, 0);
    }

    public static void glEnableVertexAttribArray(int index) {
        GLES20.glEnableVertexAttribArray(index);
    }

    public static void glDisableVertexAttribArray(int index) {
        GLES20.glDisableVertexAttribArray(index);
    }

    /** Reads from the bound array buffer, starting {@code pointer} bytes in. */
    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride,
            long pointer) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, (int) pointer);
    }

    public static void glVertexAttrib2f(int index, float x, float y) {
        GLES20.glVertexAttrib2f(index, x, y);
    }

    public static void glVertexAttribDivisor(int index, int divisor) {
        GLES30.glVertexAttribDivisor(index, divisor);
    }

    public static void glDrawArraysInstanced(int mode, int first, int count, int primcount) {
        GLES30.glDrawArraysInstanced(mode, first, count, primcount);
    }

    public static void glDrawElementsInstanced(int mode, int count, int type, long indices, int primcount) {
        GLES30.glDrawElementsInstanced(mode, count, type, (int) indices, primcount);
    }

    // --- shaders ---

    public static int glCreateShader(int type) {
        return GLES20.glCreateShader(type);
    }

    public static void glShaderSource(int shader, CharSequence source) {
        GLES20.glShaderSource(shader, source.toString());
    }

    public static void glCompileShader(int shader) {
        GLES20.glCompileShader(shader);
    }

    public static int glGetShaderi(int shader, int pname) {
        int[] value = new int[1];
        GLES20.glGetShaderiv(shader, pname, value, 0);
        return value[0];
    }

    public static String glGetShaderInfoLog(int shader, int maxLength) {
        return GLES20.glGetShaderInfoLog(shader);
    }

    public static void glDeleteShader(int shader) {
        GLES20.glDeleteShader(shader);
    }

    public static int glCreateProgram() {
        return GLES20.glCreateProgram();
    }

    public static void glAttachShader(int program, int shader) {
        GLES20.glAttachShader(program, shader);
    }

    public static void glDetachShader(int program, int shader) {
        GLES20.glDetachShader(program, shader);
    }

    public static void glLinkProgram(int program) {
        GLES20.glLinkProgram(program);
    }

    public static int glGetProgrami(int program, int pname) {
        int[] value = new int[1];
        GLES20.glGetProgramiv(program, pname, value, 0);
        return value[0];
    }

    public static String glGetProgramInfoLog(int program, int maxLength) {
        return GLES20.glGetProgramInfoLog(program);
    }

    public static void glDeleteProgram(int program) {
        GLES20.glDeleteProgram(program);
    }

    public static void glUseProgram(int program) {
        GLES20.glUseProgram(program);
    }

    public static void glBindAttribLocation(int program, int index, CharSequence name) {
        GLES20.glBindAttribLocation(program, index, name.toString());
    }

    /** No-op: ES shaders assign fragment outputs with {@code layout(location)}. */
    public static void glBindFragDataLocation(int program, int colorNumber, CharSequence name) {
    }

    public static int glGetAttribLocation(int program, CharSequence name) {
        return GLES20.glGetAttribLocation(program, name.toString());
    }

    public static int glGetUniformLocation(int program, CharSequence name) {
        return GLES20.glGetUniformLocation(program, name.toString());
    }

    public static int glGetUniformBlockIndex(int program, CharSequence name) {
        return GLES30.glGetUniformBlockIndex(program, name.toString());
    }

    public static void glUniformBlockBinding(int program, int blockIndex, int blockBinding) {
        GLES30.glUniformBlockBinding(program, blockIndex, blockBinding);
    }

    public static void glUniform1f(int location, float v0) {
        GLES20.glUniform1f(location, v0);
    }

    public static void glUniform1i(int location, int v0) {
        GLES20.glUniform1i(location, v0);
    }

    public static void glUniform1iv(int location, int[] values) {
        GLES20.glUniform1iv(location, values.length, values, 0);
    }

    public static void glUniform1iv(int location, IntBuffer values) {
        GLES20.glUniform1iv(location, values.remaining(), values);
    }

    public static void glUniform2f(int location, float v0, float v1) {
        GLES20.glUniform2f(location, v0, v1);
    }

    public static void glUniform3f(int location, float v0, float v1, float v2) {
        GLES20.glUniform3f(location, v0, v1, v2);
    }

    public static void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        GLES20.glUniform4f(location, v0, v1, v2, v3);
    }

    public static void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        GLES30.glUniformMatrix4fv(location, value.remaining() / 16, transpose, value);
    }

    public static void glProgramUniform1i(int program, int location, int v0) {
        if (GL.getCapabilities().OpenGL31) {
            GLES31.glProgramUniform1i(program, location, v0);
            return;
        }
        int[] current = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, current, 0);
        GLES20.glUseProgram(program);
        GLES20.glUniform1i(location, v0);
        GLES20.glUseProgram(current[0]);
    }

    // --- framebuffers ---

    public static int glGenFramebuffers() {
        int[] id = new int[1];
        GLES20.glGenFramebuffers(1, id, 0);
        return id[0];
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        GLES20.glBindFramebuffer(target, framebuffer);
    }

    public static void glDeleteFramebuffers(int framebuffer) {
        GLES20.glDeleteFramebuffers(1, new int[]{framebuffer}, 0);
    }

    public static int glCheckFramebufferStatus(int target) {
        return GLES20.glCheckFramebufferStatus(target);
    }

    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    /** Maps the desktop {@code GL_BACK_LEFT} onto the ES default framebuffer's {@code GL_BACK}. */
    public static int glGetFramebufferAttachmentParameteri(int target, int attachment, int pname) {
        int[] value = new int[1];
        GLES20.glGetFramebufferAttachmentParameteriv(target, attachment == GL_BACK_LEFT ? GL_BACK : attachment, pname,
                value, 0);
        return value[0];
    }

    // --- GL 4.0 ---

    /** Below ES 3.2 there is one blend function for all draw buffers, so only the first buffer's is applied. */
    public static void glBlendFunci(int buf, int sfactor, int dfactor) {
        if (GL.getCapabilities().OpenGL32) {
            GLES32.glBlendFunci(buf, sfactor, dfactor);
        } else if (buf == 0) {
            GLES20.glBlendFunc(sfactor, dfactor);
        }
    }
}
