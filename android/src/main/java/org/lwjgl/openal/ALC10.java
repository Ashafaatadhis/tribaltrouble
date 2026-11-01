package org.lwjgl.openal;

import java.nio.ByteBuffer;

/** One fixed device and one context; opening either always succeeds. */
public class ALC10 {
    public static final int ALC_INVALID = -1;
    public static final int ALC_FALSE = 0;
    public static final int ALC_TRUE = 1;
    public static final int ALC_FREQUENCY = 4103;
    public static final int ALC_REFRESH = 4104;
    public static final int ALC_SYNC = 4105;
    public static final int ALC_NO_ERROR = 0;
    public static final int ALC_INVALID_DEVICE = 40961;
    public static final int ALC_INVALID_CONTEXT = 40962;
    public static final int ALC_INVALID_ENUM = 40963;
    public static final int ALC_INVALID_VALUE = 40964;
    public static final int ALC_OUT_OF_MEMORY = 40965;
    public static final int ALC_DEFAULT_DEVICE_SPECIFIER = 4100;
    public static final int ALC_DEVICE_SPECIFIER = 4101;
    public static final int ALC_EXTENSIONS = 4102;
    public static final int ALC_MAJOR_VERSION = 4096;
    public static final int ALC_MINOR_VERSION = 4097;
    public static final int ALC_ATTRIBUTES_SIZE = 4098;
    public static final int ALC_ALL_ATTRIBUTES = 4099;

    private static final long DEVICE = 1L;
    private static final long CONTEXT = 1L;
    private static final String DEVICE_NAME = "Android";

    protected ALC10() {
    }

    public static long alcOpenDevice(CharSequence deviceName) {
        return DEVICE;
    }

    public static long alcOpenDevice(ByteBuffer deviceName) {
        return DEVICE;
    }

    public static boolean alcCloseDevice(long device) {
        return true;
    }

    public static long alcCreateContext(long device, int[] attributes) {
        return CONTEXT;
    }

    public static void alcDestroyContext(long context) {
        AudioMixer.INSTANCE.makeCurrent(0);
    }

    public static boolean alcMakeContextCurrent(long context) {
        AudioMixer.INSTANCE.makeCurrent(context);
        return true;
    }

    public static long alcGetCurrentContext() {
        return AudioMixer.INSTANCE.currentContext();
    }

    public static String alcGetString(long device, int param) {
        return switch (param) {
            case ALC_DEFAULT_DEVICE_SPECIFIER, ALC_DEVICE_SPECIFIER -> DEVICE_NAME;
            default -> "";
        };
    }

    public static boolean alcIsExtensionPresent(long device, CharSequence extension) {
        return false;
    }
}
