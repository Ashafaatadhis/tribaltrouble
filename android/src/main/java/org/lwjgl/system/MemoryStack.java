package org.lwjgl.system;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Per-thread scratch memory, so short-lived GL argument buffers are not allocated on every call. Like LWJGL's, the
 * returned buffers are not cleared and are only valid until the frame that created them is closed.
 */
public final class MemoryStack implements AutoCloseable {
    private static final int STACK_SIZE_BYTES = 64 * 1024;
    private static final int INITIAL_FRAME_CAPACITY = 8;

    private static final ThreadLocal<MemoryStack> THREAD_STACK = ThreadLocal.withInitial(MemoryStack::new);

    private final ByteBuffer memory = ByteBuffer.allocateDirect(STACK_SIZE_BYTES).order(ByteOrder.nativeOrder());
    private int[] frames = new int[INITIAL_FRAME_CAPACITY];
    private int frameCount;
    private int pointer;

    private MemoryStack() {
    }

    public static MemoryStack stackPush() {
        MemoryStack stack = THREAD_STACK.get();
        if (stack.frameCount == stack.frames.length) {
            stack.frames = Arrays.copyOf(stack.frames, stack.frameCount * 2);
        }
        stack.frames[stack.frameCount++] = stack.pointer;
        return stack;
    }

    @Override
    public void close() {
        if (frameCount == 0) {
            throw new IllegalStateException("Cannot close a memory stack frame that was not pushed");
        }
        pointer = frames[--frameCount];
    }

    public IntBuffer mallocInt(int size) {
        return allocate(size * Integer.BYTES, Integer.BYTES).asIntBuffer();
    }

    public FloatBuffer mallocFloat(int size) {
        return allocate(size * Float.BYTES, Float.BYTES).asFloatBuffer();
    }

    private ByteBuffer allocate(int bytes, int alignment) {
        int start = (pointer + alignment - 1) & -alignment;
        if (start + bytes > STACK_SIZE_BYTES) {
            throw new OutOfMemoryError("Out of stack space");
        }
        pointer = start + bytes;
        ByteBuffer view = memory.duplicate();
        view.limit(start + bytes);
        view.position(start);
        return view.slice().order(ByteOrder.nativeOrder());
    }
}
