package org.lwjgl.system.libc;

import java.nio.Buffer;

public final class LibCStdlib {
    private LibCStdlib() {
    }

    /** Decoded audio lives on the Java heap here, so there is nothing to free. */
    public static void free(Buffer buffer) {
    }
}
