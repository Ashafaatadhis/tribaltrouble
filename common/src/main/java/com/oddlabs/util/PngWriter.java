package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

/**
 * Writes 8-bit RGBA PNG files. It exists so image export does not need {@code javax.imageio}, which Android lacks.
 */
public final class PngWriter {
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int BIT_DEPTH = 8;
    private static final int COLOR_TYPE_RGBA = 6;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int FILTER_NONE = 0;

    private PngWriter() {
    }

    /**
     * @param rgba pixels in row order from the top, four bytes per pixel in red, green, blue, alpha order
     */
    public static void write(@NonNull OutputStream out, int width, int height,
            byte @NonNull [] rgba) throws IOException {
        if (rgba.length != width * height * BYTES_PER_PIXEL) {
            throw new IllegalArgumentException(
                    "Expected " + width * height * BYTES_PER_PIXEL + " bytes for " + width + "x" + height + " but got " + rgba.length);
        }
        out.write(SIGNATURE);

        ByteArrayOutputStream header = new ByteArrayOutputStream();
        DataOutputStream headerData = new DataOutputStream(header);
        headerData.writeInt(width);
        headerData.writeInt(height);
        headerData.writeByte(BIT_DEPTH);
        headerData.writeByte(COLOR_TYPE_RGBA);
        headerData.writeByte(0);
        headerData.writeByte(0);
        headerData.writeByte(0);
        writeChunk(out, "IHDR", header.toByteArray());

        ByteArrayOutputStream pixels = new ByteArrayOutputStream();
        int stride = width * BYTES_PER_PIXEL;
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(pixels)) {
            for (int y = 0; y < height; y++) {
                deflater.write(FILTER_NONE);
                deflater.write(rgba, y * stride, stride);
            }
        }
        writeChunk(out, "IDAT", pixels.toByteArray());
        writeChunk(out, "IEND", new byte[0]);
    }

    private static void writeChunk(OutputStream out, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        DataOutputStream chunk = new DataOutputStream(out);
        chunk.writeInt(data.length);
        chunk.write(typeBytes);
        chunk.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        chunk.writeInt((int) crc.getValue());
    }
}
