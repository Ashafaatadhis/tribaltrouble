package org.lwjgl.stb;

import android.media.MediaCodec;
import android.media.MediaDataSource;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Decodes an in-memory Ogg Vorbis file to 16-bit PCM as it is read, using the platform's codec. */
final class VorbisStream implements AutoCloseable {
    private static final long DEQUEUE_TIMEOUT_US = 10_000;

    private final MediaExtractor extractor = new MediaExtractor();
    private final MediaCodec codec;
    private final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    private int channels;
    private int sampleRate;
    private short[] chunk = new short[0];
    private int chunkPosition;
    private boolean inputDone;
    private boolean outputDone;

    VorbisStream(ByteBuffer encoded) throws IOException {
        byte[] bytes = new byte[encoded.remaining()];
        encoded.duplicate().get(bytes);
        extractor.setDataSource(new MediaDataSource() {
            @Override
            public int readAt(long position, byte[] buffer, int offset, int size) {
                if (position >= bytes.length) {
                    return -1;
                }
                int count = (int) Math.min(size, bytes.length - position);
                System.arraycopy(bytes, (int) position, buffer, offset, count);
                return count;
            }

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public void close() {
            }
        });
        if (extractor.getTrackCount() == 0) {
            throw new IOException("No audio track");
        }
        MediaFormat format = extractor.getTrackFormat(0);
        extractor.selectTrack(0);
        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        codec.configure(format, null, null, 0);
        codec.start();
    }

    int channels() {
        return channels;
    }

    int sampleRate() {
        return sampleRate;
    }

    /** Copies up to {@code max} decoded shorts to {@code out}; returns 0 at the end of the stream. */
    int read(short[] out, int offset, int max) {
        int written = 0;
        while (written < max) {
            if (chunkPosition >= chunk.length && !decodeMore()) {
                break;
            }
            int count = Math.min(max - written, chunk.length - chunkPosition);
            System.arraycopy(chunk, chunkPosition, out, offset + written, count);
            chunkPosition += count;
            written += count;
        }
        return written;
    }

    private boolean decodeMore() {
        while (!outputDone) {
            if (!inputDone) {
                int index = codec.dequeueInputBuffer(0);
                if (index >= 0) {
                    ByteBuffer input = codec.getInputBuffer(index);
                    int size = extractor.readSampleData(input, 0);
                    if (size < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        codec.queueInputBuffer(index, 0, size, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }
            int output = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US);
            if (output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = codec.getOutputFormat();
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            } else if (output >= 0) {
                ByteBuffer pcm = codec.getOutputBuffer(output);
                pcm.position(info.offset).limit(info.offset + info.size);
                chunk = new short[info.size / Short.BYTES];
                pcm.order(ByteOrder.nativeOrder()).asShortBuffer().get(chunk);
                chunkPosition = 0;
                outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                codec.releaseOutputBuffer(output, false);
                if (chunk.length > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void close() {
        codec.stop();
        codec.release();
        extractor.release();
    }
}
