package org.lwjgl.stb;

import org.lwjgl.system.MemoryStack;

public final class STBVorbisInfo {
    private int channels;
    private int sampleRate;

    private STBVorbisInfo() {
    }

    public static STBVorbisInfo malloc(MemoryStack stack) {
        return new STBVorbisInfo();
    }

    void set(int channels, int sampleRate) {
        this.channels = channels;
        this.sampleRate = sampleRate;
    }

    public int channels() {
        return channels;
    }

    public int sample_rate() {
        return sampleRate;
    }
}
