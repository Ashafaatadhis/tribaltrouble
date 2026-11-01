package org.lwjgl.stb;

import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

/** The part of stb_vorbis the client uses, decoding with the platform's Vorbis codec. */
public final class STBVorbis {
    private static final int VORBIS_FILE_ERROR = 1;
    private static final int DECODE_CHUNK_SHORTS = 16 * 1024;

    private static final Map<Long, VorbisStream> STREAMS = new HashMap<>();
    private static long nextHandle = 1;

    private STBVorbis() {
    }

    /** Opens a stream for reading, or returns 0 and sets the error. */
    public static long stb_vorbis_open_memory(ByteBuffer data, IntBuffer error, STBVorbisAlloc alloc) {
        try {
            VorbisStream stream = new VorbisStream(data);
            synchronized (STREAMS) {
                long handle = nextHandle++;
                STREAMS.put(handle, stream);
                return handle;
            }
        } catch (IOException | RuntimeException e) {
            if (error != null) {
                error.put(error.position(), VORBIS_FILE_ERROR);
            }
            return 0;
        }
    }

    public static void stb_vorbis_get_info(long handle, STBVorbisInfo info) {
        VorbisStream stream = stream(handle);
        info.set(stream.channels(), stream.sampleRate());
    }

    /**
     * Decodes into the buffer from its position without moving it, as the native function does.
     *
     * @return the number of samples per channel written
     */
    public static int stb_vorbis_get_samples_short_interleaved(long handle, int channels, ShortBuffer buffer) {
        VorbisStream stream = stream(handle);
        short[] decoded = new short[buffer.remaining() / channels * channels];
        int shorts = stream.read(decoded, 0, decoded.length);
        for (int i = 0; i < shorts; i++) {
            buffer.put(buffer.position() + i, decoded[i]);
        }
        return shorts / channels;
    }

    public static void stb_vorbis_close(long handle) {
        VorbisStream stream;
        synchronized (STREAMS) {
            stream = STREAMS.remove(handle);
        }
        if (stream != null) {
            stream.close();
        }
    }

    /** Decodes the whole file; returns null on failure. */
    public static ShortBuffer stb_vorbis_decode_memory(ByteBuffer data, IntBuffer channels, IntBuffer sampleRate) {
        try (VorbisStream stream = new VorbisStream(data)) {
            short[] all = new short[DECODE_CHUNK_SHORTS];
            int length = 0;
            int read;
            do {
                if (length + DECODE_CHUNK_SHORTS > all.length) {
                    all = java.util.Arrays.copyOf(all, all.length * 2);
                }
                read = stream.read(all, length, DECODE_CHUNK_SHORTS);
                length += read;
            } while (read > 0);
            channels.put(channels.position(), stream.channels());
            sampleRate.put(sampleRate.position(), stream.sampleRate());
            ShortBuffer result = BufferUtils.createShortBuffer(length);
            result.put(all, 0, length).flip();
            return result;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static VorbisStream stream(long handle) {
        synchronized (STREAMS) {
            VorbisStream stream = STREAMS.get(handle);
            if (stream == null) {
                throw new IllegalArgumentException("No such stream: " + handle);
            }
            return stream;
        }
    }
}
