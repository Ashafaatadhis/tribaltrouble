package org.lwjgl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** Direct, zeroed, native-order buffers, which is what the GL calls require. */
public final class BufferUtils {
    private BufferUtils() {
    }

    public static ByteBuffer createByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    public static ShortBuffer createShortBuffer(int capacity) {
        return createByteBuffer(capacity * Short.BYTES).asShortBuffer();
    }

    public static IntBuffer createIntBuffer(int capacity) {
        return createByteBuffer(capacity * Integer.BYTES).asIntBuffer();
    }

    public static FloatBuffer createFloatBuffer(int capacity) {
        return createByteBuffer(capacity * Float.BYTES).asFloatBuffer();
    }
}
