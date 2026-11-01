package org.lwjgl.openal;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A software mixer behind the OpenAL calls the game makes, playing through one {@link AudioTrack}. It implements the
 * parts of OpenAL the game uses: static and queued buffers, 3D sources with the inverse distance clamped model, and a
 * listener. Every method takes the lock, as the engine thread calls in while the mixer thread reads.
 */
final class AudioMixer {
    static final AudioMixer INSTANCE = new AudioMixer();

    private static final int OUTPUT_RATE = 44_100;
    private static final int CHUNK_FRAMES = 1024;
    private static final int TRACK_CHUNKS = 4;
    private static final long IDLE_BEFORE_PAUSE_NANOS = 2_000_000_000L;
    private static final float SHORT_SCALE = 32768f;
    private static final float QUARTER_PI = (float) (Math.PI / 4);

    private static final class PcmBuffer {
        final short[] samples;
        final int channels;
        final int rate;
        final int frames;

        PcmBuffer(short[] samples, int channels, int rate) {
            this.samples = samples;
            this.channels = channels;
            this.rate = rate;
            this.frames = samples.length / channels;
        }
    }

    private static final class Source {
        float gain = 1f;
        float pitch = 1f;
        float minGain;
        float maxGain = 1f;
        float rolloff = 1f;
        float referenceDistance = 1f;
        float maxDistance = Float.MAX_VALUE;
        float x;
        float y;
        float z;
        boolean relative;
        boolean looping;
        int state = AL10.AL_INITIAL;
        final List<Integer> queue = new ArrayList<>();
        int processed;
        int current;
        double cursor;
        float rampedGainLeft;
        float rampedGainRight;
        boolean ramped;
    }

    private final Map<Integer, PcmBuffer> buffers = new HashMap<>();
    private final Map<Integer, Source> sources = new HashMap<>();
    private int nextBufferId = 1;
    private int nextSourceId = 1;
    private int error = AL10.AL_NO_ERROR;

    private float listenerGain = 1f;
    private float listenerX;
    private float listenerY;
    private float listenerZ;
    private float forwardX;
    private float forwardY;
    private float forwardZ = -1f;
    private float upX;
    private float upY = 1f;
    private float upZ;

    private long currentContext;
    private Thread thread;
    private boolean running;

    private AudioMixer() {
    }

    // --- context ---

    synchronized void makeCurrent(long context) {
        currentContext = context;
        if (context != 0 && thread == null) {
            running = true;
            thread = new Thread(this::run, "audio-mixer");
            thread.setDaemon(true);
            thread.start();
        } else if (context == 0 && thread != null) {
            running = false;
            notifyAll();
            thread = null;
        }
    }

    synchronized long currentContext() {
        return currentContext;
    }

    synchronized int takeError() {
        int result = error;
        error = AL10.AL_NO_ERROR;
        return result;
    }

    private void fail(int code) {
        if (error == AL10.AL_NO_ERROR) {
            error = code;
        }
    }

    // --- buffers ---

    synchronized int genBuffer() {
        int id = nextBufferId++;
        buffers.put(id, new PcmBuffer(new short[0], 1, OUTPUT_RATE));
        return id;
    }

    synchronized void deleteBuffer(int id) {
        buffers.remove(id);
    }

    synchronized boolean isBuffer(int id) {
        return id == 0 || buffers.containsKey(id);
    }

    synchronized void bufferData(int id, int format, short[] samples, int rate) {
        if (!buffers.containsKey(id)) {
            fail(AL10.AL_INVALID_NAME);
            return;
        }
        int channels = switch (format) {
            case AL10.AL_FORMAT_MONO16 -> 1;
            case AL10.AL_FORMAT_STEREO16 -> 2;
            default -> 0;
        };
        if (channels == 0) {
            fail(AL10.AL_INVALID_ENUM);
            return;
        }
        buffers.put(id, new PcmBuffer(samples, channels, rate));
    }

    // --- sources ---

    synchronized int genSource() {
        int id = nextSourceId++;
        sources.put(id, new Source());
        return id;
    }

    synchronized void deleteSource(int id) {
        sources.remove(id);
    }

    synchronized boolean isSource(int id) {
        return sources.containsKey(id);
    }

    private Source source(int id) {
        Source source = sources.get(id);
        if (source == null) {
            fail(AL10.AL_INVALID_NAME);
        }
        return source;
    }

    synchronized void setSourceInt(int id, int param, int value) {
        Source s = source(id);
        if (s == null) {
            return;
        }
        switch (param) {
            case AL10.AL_BUFFER -> {
                s.queue.clear();
                if (value != 0) {
                    s.queue.add(value);
                }
                rewind(s);
            }
            case AL10.AL_LOOPING -> s.looping = value != 0;
            case AL10.AL_SOURCE_RELATIVE -> s.relative = value != 0;
            default -> {
            }
        }
    }

    synchronized void setSourceFloat(int id, int param, float value) {
        Source s = source(id);
        if (s == null) {
            return;
        }
        switch (param) {
            case AL10.AL_GAIN -> s.gain = value;
            case AL10.AL_PITCH -> s.pitch = value;
            case AL10.AL_MIN_GAIN -> s.minGain = value;
            case AL10.AL_MAX_GAIN -> s.maxGain = value;
            case AL10.AL_ROLLOFF_FACTOR -> s.rolloff = value;
            case AL10.AL_REFERENCE_DISTANCE -> s.referenceDistance = value;
            case AL10.AL_MAX_DISTANCE -> s.maxDistance = value;
            default -> {
            }
        }
    }

    synchronized void setSourcePosition(int id, float x, float y, float z) {
        Source s = source(id);
        if (s != null) {
            s.x = x;
            s.y = y;
            s.z = z;
        }
    }

    synchronized float[] getSourcePosition(int id) {
        Source s = source(id);
        return s == null ? new float[3] : new float[]{s.x, s.y, s.z};
    }

    synchronized int getSourceInt(int id, int param) {
        Source s = source(id);
        if (s == null) {
            return 0;
        }
        return switch (param) {
            case AL10.AL_SOURCE_STATE -> s.state;
            case AL10.AL_BUFFERS_PROCESSED -> s.processed;
            case AL10.AL_BUFFERS_QUEUED -> s.queue.size();
            case AL10.AL_BUFFER -> s.queue.isEmpty() ? 0 : s.queue.get(Math.min(s.current, s.queue.size() - 1));
            case AL10.AL_LOOPING -> s.looping ? 1 : 0;
            default -> 0;
        };
    }

    synchronized void play(int id) {
        Source s = source(id);
        if (s == null) {
            return;
        }
        if (s.state == AL10.AL_STOPPED) {
            rewind(s);
        }
        s.state = s.queue.isEmpty() ? AL10.AL_STOPPED : AL10.AL_PLAYING;
        notifyAll();
    }

    synchronized void pause(int id) {
        Source s = source(id);
        if (s != null && s.state == AL10.AL_PLAYING) {
            s.state = AL10.AL_PAUSED;
        }
    }

    synchronized void stop(int id) {
        Source s = source(id);
        if (s != null && s.state != AL10.AL_INITIAL) {
            s.state = AL10.AL_STOPPED;
            s.processed = s.queue.size();
            s.current = s.queue.size();
        }
    }

    synchronized void rewind(int id) {
        Source s = source(id);
        if (s != null) {
            rewind(s);
        }
    }

    private static void rewind(Source s) {
        s.state = AL10.AL_INITIAL;
        s.processed = 0;
        s.current = 0;
        s.cursor = 0;
    }

    synchronized void queueBuffers(int id, int[] bufferIds) {
        Source s = source(id);
        if (s == null) {
            return;
        }
        for (int bufferId : bufferIds) {
            if (!buffers.containsKey(bufferId)) {
                fail(AL10.AL_INVALID_NAME);
                return;
            }
            s.queue.add(bufferId);
        }
    }

    /** Removes the first {@code count} processed buffers from the queue and returns their names. */
    synchronized int[] unqueueBuffers(int id, int count) {
        Source s = source(id);
        if (s == null) {
            return new int[count];
        }
        if (count > s.processed) {
            fail(AL10.AL_INVALID_VALUE);
            return new int[count];
        }
        int[] removed = new int[count];
        for (int i = 0; i < count; i++) {
            removed[i] = s.queue.remove(0);
        }
        s.processed -= count;
        s.current = Math.max(0, s.current - count);
        return removed;
    }

    // --- listener ---

    synchronized void setListenerGain(float gain) {
        listenerGain = gain;
    }

    synchronized void setListenerPosition(float x, float y, float z) {
        listenerX = x;
        listenerY = y;
        listenerZ = z;
    }

    synchronized void setListenerOrientation(float[] atAndUp) {
        forwardX = atAndUp[0];
        forwardY = atAndUp[1];
        forwardZ = atAndUp[2];
        upX = atAndUp[3];
        upY = atAndUp[4];
        upZ = atAndUp[5];
    }

    // --- mixing ---

    private void run() {
        AudioTrack track = createTrack();
        short[] out = new short[CHUNK_FRAMES * 2];
        float[] left = new float[CHUNK_FRAMES];
        float[] right = new float[CHUNK_FRAMES];
        long idleSince = System.nanoTime();
        boolean paused = false;
        track.play();
        try {
            while (true) {
                boolean audible;
                synchronized (this) {
                    if (!running) {
                        return;
                    }
                    audible = mix(left, right);
                }
                long now = System.nanoTime();
                if (audible) {
                    idleSince = now;
                } else if (now - idleSince > IDLE_BEFORE_PAUSE_NANOS) {
                    track.pause();
                    paused = true;
                    synchronized (this) {
                        while (running && !anyPlaying()) {
                            wait();
                        }
                    }
                    idleSince = System.nanoTime();
                    continue;
                }
                if (paused) {
                    track.play();
                    paused = false;
                }
                for (int i = 0; i < CHUNK_FRAMES; i++) {
                    out[2 * i] = clip(left[i]);
                    out[2 * i + 1] = clip(right[i]);
                }
                track.write(out, 0, out.length);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            track.release();
        }
    }

    private static AudioTrack createTrack() {
        int minimum = AudioTrack.getMinBufferSize(OUTPUT_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        return new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(
                AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(
                        OUTPUT_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(Math.max(minimum, CHUNK_FRAMES * 4 * TRACK_CHUNKS)).setTransferMode(
                        AudioTrack.MODE_STREAM).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build();
    }

    private static short clip(float sample) {
        return (short) (Math.max(-1f, Math.min(1f, sample)) * (SHORT_SCALE - 1));
    }

    private boolean anyPlaying() {
        for (Source s : sources.values()) {
            if (s.state == AL10.AL_PLAYING) {
                return true;
            }
        }
        return false;
    }

    /** Mixes one chunk of every playing source; the caller holds the lock. */
    private boolean mix(float[] left, float[] right) {
        java.util.Arrays.fill(left, 0f);
        java.util.Arrays.fill(right, 0f);
        boolean audible = false;
        for (Source s : sources.values()) {
            if (s.state == AL10.AL_PLAYING) {
                mixSource(s, left, right);
                audible = true;
            }
        }
        return audible;
    }

    private void mixSource(Source s, float[] left, float[] right) {
        PcmBuffer first = currentBuffer(s);
        if (first == null) {
            s.state = AL10.AL_STOPPED;
            return;
        }
        float targetLeft;
        float targetRight;
        if (first.channels == 1) {
            float[] gains = spatialGains(s);
            targetLeft = gains[0];
            targetRight = gains[1];
        } else {
            targetLeft = targetRight = clampGain(s, s.gain) * listenerGain;
        }
        if (!s.ramped) {
            s.rampedGainLeft = targetLeft;
            s.rampedGainRight = targetRight;
            s.ramped = true;
        }
        float stepLeft = (targetLeft - s.rampedGainLeft) / CHUNK_FRAMES;
        float stepRight = (targetRight - s.rampedGainRight) / CHUNK_FRAMES;
        float gainLeft = s.rampedGainLeft;
        float gainRight = s.rampedGainRight;

        for (int i = 0; i < CHUNK_FRAMES; i++) {
            PcmBuffer b = currentBuffer(s);
            if (b == null) {
                break;
            }
            int index = (int) s.cursor;
            float fraction = (float) (s.cursor - index);
            float sampleLeft;
            float sampleRight;
            if (b.channels == 1) {
                float a = b.samples[index];
                float c = index + 1 < b.frames ? b.samples[index + 1] : a;
                sampleLeft = sampleRight = (a + (c - a) * fraction) / SHORT_SCALE;
            } else {
                int next = index + 1 < b.frames ? index + 1 : index;
                float al = b.samples[2 * index];
                float ar = b.samples[2 * index + 1];
                sampleLeft = (al + (b.samples[2 * next] - al) * fraction) / SHORT_SCALE;
                sampleRight = (ar + (b.samples[2 * next + 1] - ar) * fraction) / SHORT_SCALE;
            }
            gainLeft += stepLeft;
            gainRight += stepRight;
            left[i] += sampleLeft * gainLeft;
            right[i] += sampleRight * gainRight;

            s.cursor += s.pitch * b.rate / OUTPUT_RATE;
            while (s.state == AL10.AL_PLAYING && s.cursor >= currentFrames(s)) {
                s.cursor -= currentFrames(s);
                advance(s);
            }
            if (s.state != AL10.AL_PLAYING) {
                break;
            }
        }
        s.rampedGainLeft = targetLeft;
        s.rampedGainRight = targetRight;
    }

    private PcmBuffer currentBuffer(Source s) {
        if (s.current >= s.queue.size()) {
            return null;
        }
        return buffers.get(s.queue.get(s.current));
    }

    private int currentFrames(Source s) {
        PcmBuffer b = currentBuffer(s);
        return b == null ? 0 : Math.max(1, b.frames);
    }

    private static void advance(Source s) {
        s.current++;
        s.processed++;
        if (s.current >= s.queue.size()) {
            if (s.looping && !s.queue.isEmpty()) {
                s.current = 0;
                s.processed = 0;
            } else {
                s.state = AL10.AL_STOPPED;
                s.processed = s.queue.size();
            }
        }
    }

    private float clampGain(Source s, float gain) {
        return Math.max(s.minGain, Math.min(s.maxGain, gain));
    }

    /** Distance attenuation and left/right panning for a mono source, as OpenAL does for the clamped inverse model. */
    private float[] spatialGains(Source s) {
        float dx = s.relative ? s.x : s.x - listenerX;
        float dy = s.relative ? s.y : s.y - listenerY;
        float dz = s.relative ? s.z : s.z - listenerZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float clamped = Math.max(s.referenceDistance, Math.min(s.maxDistance, distance));
        float attenuation = s.referenceDistance / (s.referenceDistance + s.rolloff * (clamped - s.referenceDistance));
        float gain = clampGain(s, attenuation * s.gain) * listenerGain;

        float pan = 0f;
        if (distance > 1e-4f) {
            float fl = (float) Math.sqrt(forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ);
            // right = forward x up
            float rx = forwardY * upZ - forwardZ * upY;
            float ry = forwardZ * upX - forwardX * upZ;
            float rz = forwardX * upY - forwardY * upX;
            float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
            if (fl > 0f && rl > 0f) {
                pan = Math.max(-1f, Math.min(1f, (dx * rx + dy * ry + dz * rz) / (distance * rl)));
            }
        }
        float angle = (pan + 1f) * QUARTER_PI;
        return new float[]{gain * (float) Math.cos(angle), gain * (float) Math.sin(angle)};
    }
}
