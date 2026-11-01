package org.lwjgl.openal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** The OpenAL 1.0 calls the game makes, served by {@link AudioMixer}. Constants have the values of the real API. */
public class AL10 {
    public static final int AL_INVALID = -1;
    public static final int AL_NONE = 0;
    public static final int AL_FALSE = 0;
    public static final int AL_TRUE = 1;
    public static final int AL_NO_ERROR = 0;
    public static final int AL_INVALID_NAME = 40961;
    public static final int AL_INVALID_ENUM = 40962;
    public static final int AL_INVALID_VALUE = 40963;
    public static final int AL_INVALID_OPERATION = 40964;
    public static final int AL_OUT_OF_MEMORY = 40965;
    public static final int AL_DOPPLER_FACTOR = 49152;
    public static final int AL_DISTANCE_MODEL = 53248;
    public static final int AL_VENDOR = 45057;
    public static final int AL_VERSION = 45058;
    public static final int AL_RENDERER = 45059;
    public static final int AL_EXTENSIONS = 45060;
    public static final int AL_INVERSE_DISTANCE = 53249;
    public static final int AL_INVERSE_DISTANCE_CLAMPED = 53250;
    public static final int AL_SOURCE_ABSOLUTE = 513;
    public static final int AL_SOURCE_RELATIVE = 514;
    public static final int AL_POSITION = 4100;
    public static final int AL_VELOCITY = 4102;
    public static final int AL_GAIN = 4106;
    public static final int AL_CONE_INNER_ANGLE = 4097;
    public static final int AL_CONE_OUTER_ANGLE = 4098;
    public static final int AL_PITCH = 4099;
    public static final int AL_DIRECTION = 4101;
    public static final int AL_LOOPING = 4103;
    public static final int AL_BUFFER = 4105;
    public static final int AL_SOURCE_STATE = 4112;
    public static final int AL_CONE_OUTER_GAIN = 4130;
    public static final int AL_SOURCE_TYPE = 4135;
    public static final int AL_INITIAL = 4113;
    public static final int AL_PLAYING = 4114;
    public static final int AL_PAUSED = 4115;
    public static final int AL_STOPPED = 4116;
    public static final int AL_ORIENTATION = 4111;
    public static final int AL_BUFFERS_QUEUED = 4117;
    public static final int AL_BUFFERS_PROCESSED = 4118;
    public static final int AL_MIN_GAIN = 4109;
    public static final int AL_MAX_GAIN = 4110;
    public static final int AL_REFERENCE_DISTANCE = 4128;
    public static final int AL_ROLLOFF_FACTOR = 4129;
    public static final int AL_MAX_DISTANCE = 4131;
    public static final int AL_FREQUENCY = 8193;
    public static final int AL_BITS = 8194;
    public static final int AL_CHANNELS = 8195;
    public static final int AL_SIZE = 8196;
    public static final int AL_FORMAT_MONO8 = 4352;
    public static final int AL_FORMAT_MONO16 = 4353;
    public static final int AL_FORMAT_STEREO8 = 4354;
    public static final int AL_FORMAT_STEREO16 = 4355;
    public static final int AL_UNUSED = 8208;
    public static final int AL_PENDING = 8209;
    public static final int AL_PROCESSED = 8210;

    private static final String VENDOR = "Android";
    private static final String RENDERER = "Software mixer";
    private static final String VERSION = "1.1 software";

    protected AL10() {
    }

    public static int alGenSources() {
        return AudioMixer.INSTANCE.genSource();
    }

    public static void alDeleteSources(int source) {
        AudioMixer.INSTANCE.deleteSource(source);
    }

    public static boolean alIsSource(int source) {
        return AudioMixer.INSTANCE.isSource(source);
    }

    public static void alSourcei(int source, int param, int value) {
        AudioMixer.INSTANCE.setSourceInt(source, param, value);
    }

    public static void alSourcef(int source, int param, float value) {
        AudioMixer.INSTANCE.setSourceFloat(source, param, value);
    }

    public static void alSource3f(int source, int param, float x, float y, float z) {
        if (param == AL_POSITION) {
            AudioMixer.INSTANCE.setSourcePosition(source, x, y, z);
        }
    }

    public static int alGetSourcei(int source, int param) {
        return AudioMixer.INSTANCE.getSourceInt(source, param);
    }

    public static void alGetSourcefv(int source, int param, FloatBuffer values) {
        if (param == AL_POSITION) {
            float[] position = AudioMixer.INSTANCE.getSourcePosition(source);
            for (int i = 0; i < position.length; i++) {
                values.put(values.position() + i, position[i]);
            }
        }
    }

    public static void alSourcePlay(int source) {
        AudioMixer.INSTANCE.play(source);
    }

    public static void alSourcePause(int source) {
        AudioMixer.INSTANCE.pause(source);
    }

    public static void alSourceStop(int source) {
        AudioMixer.INSTANCE.stop(source);
    }

    public static void alSourceRewind(int source) {
        AudioMixer.INSTANCE.rewind(source);
    }

    public static void alSourceQueueBuffers(int source, IntBuffer buffers) {
        AudioMixer.INSTANCE.queueBuffers(source, toArray(buffers));
    }

    public static void alSourceUnqueueBuffers(int source, IntBuffer buffers) {
        int[] removed = AudioMixer.INSTANCE.unqueueBuffers(source, buffers.remaining());
        for (int i = 0; i < removed.length; i++) {
            buffers.put(buffers.position() + i, removed[i]);
        }
    }

    public static void alGenBuffers(IntBuffer buffers) {
        for (int i = 0; i < buffers.remaining(); i++) {
            buffers.put(buffers.position() + i, AudioMixer.INSTANCE.genBuffer());
        }
    }

    public static int alGenBuffers() {
        return AudioMixer.INSTANCE.genBuffer();
    }

    public static void alDeleteBuffers(IntBuffer buffers) {
        for (int id : toArray(buffers)) {
            AudioMixer.INSTANCE.deleteBuffer(id);
        }
    }

    public static void alDeleteBuffers(int buffer) {
        AudioMixer.INSTANCE.deleteBuffer(buffer);
    }

    public static boolean alIsBuffer(int buffer) {
        return AudioMixer.INSTANCE.isBuffer(buffer);
    }

    public static void alBufferData(int buffer, int format, ShortBuffer data, int frequency) {
        short[] samples = new short[data.remaining()];
        data.duplicate().get(samples);
        AudioMixer.INSTANCE.bufferData(buffer, format, samples, frequency);
    }

    /** 16-bit data in a byte buffer is read in the buffer's byte order, which is native for the buffers made here. */
    public static void alBufferData(int buffer, int format, ByteBuffer data, int frequency) {
        ByteBuffer source = data.duplicate();
        if (format == AL_FORMAT_MONO8 || format == AL_FORMAT_STEREO8) {
            short[] samples = new short[source.remaining()];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = (short) (((source.get(source.position() + i) & 0xFF) - 128) << 8);
            }
            AudioMixer.INSTANCE.bufferData(buffer, format == AL_FORMAT_MONO8 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16,
                    samples, frequency);
            return;
        }
        short[] samples = new short[source.remaining() / Short.BYTES];
        source.order(ByteOrder.nativeOrder()).asShortBuffer().get(samples);
        AudioMixer.INSTANCE.bufferData(buffer, format, samples, frequency);
    }

    public static void alListenerf(int param, float value) {
        if (param == AL_GAIN) {
            AudioMixer.INSTANCE.setListenerGain(value);
        }
    }

    public static void alListener3f(int param, float x, float y, float z) {
        if (param == AL_POSITION) {
            AudioMixer.INSTANCE.setListenerPosition(x, y, z);
        }
    }

    public static void alListenerfv(int param, FloatBuffer values) {
        if (param == AL_ORIENTATION) {
            float[] orientation = new float[6];
            for (int i = 0; i < orientation.length; i++) {
                orientation[i] = values.get(values.position() + i);
            }
            AudioMixer.INSTANCE.setListenerOrientation(orientation);
        }
    }

    public static void alDistanceModel(int model) {
    }

    public static int alGetError() {
        return AudioMixer.INSTANCE.takeError();
    }

    public static String alGetString(int param) {
        return switch (param) {
            case AL_VENDOR -> VENDOR;
            case AL_RENDERER -> RENDERER;
            case AL_VERSION -> VERSION;
            default -> "";
        };
    }

    private static int[] toArray(IntBuffer buffer) {
        int[] values = new int[buffer.remaining()];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.get(buffer.position() + i);
        }
        return values;
    }

}
