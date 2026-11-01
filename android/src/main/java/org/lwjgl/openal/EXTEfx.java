package org.lwjgl.openal;

/** Effects are not available in the mixer: the calls hand out names and do nothing. */
public final class EXTEfx {
    public static final int AL_AUXILIARY_SEND_FILTER = 131078;
    public static final int AL_DIRECT_FILTER = 131077;
    public static final int AL_EAXREVERB_AIR_ABSORPTION_GAINHF = 19;
    public static final int AL_EAXREVERB_DECAY_TIME = 6;
    public static final int AL_EAXREVERB_DENSITY = 1;
    public static final int AL_EAXREVERB_DIFFUSION = 2;
    public static final int AL_EAXREVERB_GAIN = 3;
    public static final int AL_EAXREVERB_GAINHF = 4;
    public static final int AL_EAXREVERB_LATE_REVERB_DELAY = 13;
    public static final int AL_EAXREVERB_REFLECTIONS_DELAY = 10;
    public static final int AL_EAXREVERB_REFLECTIONS_GAIN = 9;
    public static final int AL_EFFECT_EAXREVERB = 32768;
    public static final int AL_EFFECT_NULL = 0;
    public static final int AL_EFFECT_TYPE = 32769;
    public static final int AL_EFFECTSLOT_AUXILIARY_SEND_AUTO = 3;
    public static final int AL_EFFECTSLOT_EFFECT = 1;
    public static final int AL_FILTER_LOWPASS = 1;
    public static final int AL_FILTER_TYPE = 32769;
    public static final int AL_LOWPASS_GAIN = 1;
    public static final int AL_LOWPASS_GAINHF = 2;

    private static int nextName = 1;

    private EXTEfx() {
    }

    public static synchronized int alGenFilters() {
        return nextName++;
    }

    public static void alFilteri(int filter, int param, int value) {
    }

    public static void alFilterf(int filter, int param, float value) {
    }

    public static void alDeleteFilters(int filter) {
    }

    public static synchronized int alGenEffects() {
        return nextName++;
    }

    public static void alEffecti(int effect, int param, int value) {
    }

    public static void alEffectf(int effect, int param, float value) {
    }

    public static void alDeleteEffects(int effect) {
    }

    public static synchronized int alGenAuxiliaryEffectSlots() {
        return nextName++;
    }

    public static void alAuxiliaryEffectSloti(int slot, int param, int value) {
    }

    public static void alDeleteAuxiliaryEffectSlots(int slot) {
    }
}
