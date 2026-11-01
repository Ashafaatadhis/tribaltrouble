package org.lwjgl.openal;

import java.util.List;

public final class ALUtil {
    private ALUtil() {
    }

    public static List<String> getStringList(long device, int token) {
        return List.of(ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER));
    }
}
