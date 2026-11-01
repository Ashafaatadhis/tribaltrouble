package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;
import org.lwjgl.openal.AL10;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class Wave {
    private static final int RIFF_HEADER_BYTES = 12;
    private static final int CHUNK_HEADER_BYTES = 8;
    private static final int TAG_BYTES = 4;
    private static final int FMT_CHUNK_MIN_BYTES = 16;
    private static final short WAVE_FORMAT_PCM = 1;

    private final ByteBuffer data;
    private final int format;
    private final int sample_rate;

    /** Thrown for anything that is not a PCM WAV file, so callers can try another decoder. */
    public static final class UnsupportedFormatException extends IOException {
        UnsupportedFormatException(@NonNull String message) {
            super(message);
        }
    }

    public Wave(@NonNull URL file) throws IOException {
        try (InputStream in = new BufferedInputStream(file.openStream())) {
            byte[] riff = in.readNBytes(RIFF_HEADER_BYTES);
            if (riff.length < RIFF_HEADER_BYTES || !hasTag(riff, 0, "RIFF") || !hasTag(riff, 8, "WAVE")) {
                throw new UnsupportedFormatException("Not a WAV file: " + file);
            }

            int channels = 0;
            int bits = 0;
            int rate = 0;
            boolean hasFormat = false;
            byte[] pcm;
            while (true) {
                byte[] header = in.readNBytes(CHUNK_HEADER_BYTES);
                if (header.length < CHUNK_HEADER_BYTES) {
                    throw new IOException("No data chunk in " + file);
                }
                long size = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4) & 0xFFFFFFFFL;
                if (hasTag(header, 0, "fmt ")) {
                    ByteBuffer fmt = ByteBuffer.wrap(in.readNBytes((int) size)).order(ByteOrder.LITTLE_ENDIAN);
                    if (fmt.remaining() < FMT_CHUNK_MIN_BYTES) {
                        throw new IOException("Truncated fmt chunk in " + file);
                    }
                    if (fmt.getShort(0) != WAVE_FORMAT_PCM) {
                        throw new UnsupportedFormatException("Not PCM audio: " + file);
                    }
                    channels = fmt.getShort(2);
                    rate = fmt.getInt(4);
                    bits = fmt.getShort(14);
                    hasFormat = true;
                    skipPadding(in, size);
                } else if (hasTag(header, 0, "data")) {
                    if (!hasFormat) {
                        throw new IOException("Data chunk before fmt chunk in " + file);
                    }
                    pcm = in.readNBytes((int) Math.min(size, Integer.MAX_VALUE));
                    break;
                } else {
                    in.skipNBytes(size + (size & 1));
                }
            }

            format = getFormat(channels, bits);
            int frame_bytes = channels * bits / 8;
            data = directWaveOrder(Arrays.copyOf(pcm, pcm.length - pcm.length % frame_bytes), bits);
            sample_rate = rate;
        }
    }

    private static boolean hasTag(byte @NonNull [] bytes, int offset, @NonNull String tag) {
        return new String(bytes, offset, TAG_BYTES, StandardCharsets.US_ASCII).equals(tag);
    }

    private static void skipPadding(@NonNull InputStream in, long chunk_size) throws IOException {
        if ((chunk_size & 1) != 0) {
            in.skipNBytes(1);
        }
    }

    public Wave(ByteBuffer data, int channels, int bitrate, int sample_rate) {
        this.data = data;
        this.sample_rate = sample_rate;
        format = getFormat(channels, bitrate);
    }

    public static int getFormat(int channels, int sample_size_in_bits) {
        if (channels == 1 && sample_size_in_bits == 8) {
            return AL10.AL_FORMAT_MONO8;
        } else if (channels == 1 && sample_size_in_bits == 16) {
            return AL10.AL_FORMAT_MONO16;
        } else if (channels == 2 && sample_size_in_bits == 8) {
            return AL10.AL_FORMAT_STEREO8;
        } else if (channels == 2 && sample_size_in_bits == 16) {
            return AL10.AL_FORMAT_STEREO16;
        } else {
            throw new RuntimeException(
                    "Unsupported wave format channels=" + channels + " sample_size_in_bits=" + sample_size_in_bits);
        }
    }

    private @NonNull ByteBuffer directWaveOrder(byte @NonNull [] buffer, int bits) {
        ByteBuffer src = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer dest = ByteBuffer.allocateDirect(buffer.length).order(ByteOrder.nativeOrder());

        if (bits == 16) {
            ShortBuffer dest_short = dest.asShortBuffer();
            ShortBuffer src_short = src.asShortBuffer();
            while (src_short.hasRemaining()) {
                dest_short.put(src_short.get());
            }
        } else {
            while (src.hasRemaining()) {
                dest.put(src.get());
            }
        }
        dest.rewind();
        return dest;
    }

    public ByteBuffer getData() {
        return data;
    }

    public int getFormat() {
        return format;
    }

    public int getSampleRate() {
        return sample_rate;
    }
}
