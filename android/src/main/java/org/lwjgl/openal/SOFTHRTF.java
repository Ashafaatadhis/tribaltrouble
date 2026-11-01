package org.lwjgl.openal;

/** Head-related transfer functions are not available in the mixer. */
public final class SOFTHRTF {
    public static final int ALC_HRTF_SOFT = 6546;
    public static final int ALC_HRTF_ID_SOFT = 6550;
    public static final int ALC_DONT_CARE_SOFT = 2;
    public static final int ALC_HRTF_STATUS_SOFT = 6547;
    public static final int ALC_NUM_HRTF_SPECIFIERS_SOFT = 6548;
    public static final int ALC_HRTF_SPECIFIER_SOFT = 6549;
    public static final int ALC_HRTF_DISABLED_SOFT = 0;
    public static final int ALC_HRTF_ENABLED_SOFT = 1;
    public static final int ALC_HRTF_DENIED_SOFT = 2;
    public static final int ALC_HRTF_REQUIRED_SOFT = 3;
    public static final int ALC_HRTF_HEADPHONES_DETECTED_SOFT = 4;
    public static final int ALC_HRTF_UNSUPPORTED_FORMAT_SOFT = 5;

    private SOFTHRTF() {
    }

    public static boolean alcResetDeviceSOFT(long device, int[] attributes) {
        return false;
    }
}
