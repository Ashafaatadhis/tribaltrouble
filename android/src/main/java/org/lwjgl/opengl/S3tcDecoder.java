package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Decodes S3TC (DXT1 and DXT5) blocks to RGBA bytes, for GPUs without the S3TC extension, which is most mobile ones.
 */
final class S3tcDecoder {
    private static final int BLOCK_SIZE = 4;
    private static final int DXT1_BLOCK_BYTES = 8;
    private static final int DXT5_BLOCK_BYTES = 16;
    private static final int ALPHA_BLOCK_BYTES = 8;
    private static final int RGBA_BYTES = 4;
    private static final int PACKED_BYTES = 2;
    private static final int OPAQUE = 255;

    private S3tcDecoder() {
    }

    /**
     * @param dxt1Alpha whether a DXT1 block's "transparent" colour is really transparent, as in the RGBA format, or
     *                  opaque black, as in the RGB format
     * @return {@code width * height} pixels, four bytes each in red, green, blue, alpha order
     */
    static ByteBuffer decode(ByteBuffer blocks, int width, int height, boolean dxt5, boolean dxt1Alpha) {
        return decode(blocks, width, height, dxt5, dxt1Alpha, Packing.RGBA8);
    }

    /** Output layouts. The 16-bit ones halve the memory of a decoded texture at some cost in colour precision. */
    enum Packing {
        RGBA8(RGBA_BYTES),
        RGB565(PACKED_BYTES),
        RGBA5551(PACKED_BYTES),
        RGBA4444(PACKED_BYTES);

        final int bytesPerPixel;

        Packing(int bytesPerPixel) {
            this.bytesPerPixel = bytesPerPixel;
        }
    }

    /** @return {@code width * height} pixels in the given layout; 16-bit pixels are native-order shorts */
    static ByteBuffer decode(ByteBuffer blocks, int width, int height, boolean dxt5, boolean dxt1Alpha,
            Packing packing) {
        ByteBuffer in = blocks.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer out = ByteBuffer.allocateDirect(width * height * packing.bytesPerPixel)
                .order(ByteOrder.nativeOrder());
        int blockBytes = dxt5 ? DXT5_BLOCK_BYTES : DXT1_BLOCK_BYTES;
        int blocksX = (width + BLOCK_SIZE - 1) / BLOCK_SIZE;
        int blocksY = (height + BLOCK_SIZE - 1) / BLOCK_SIZE;
        int[] alpha = new int[BLOCK_SIZE * BLOCK_SIZE];
        int[] colour = new int[BLOCK_SIZE * BLOCK_SIZE];
        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                int start = in.position() + (by * blocksX + bx) * blockBytes;
                if (dxt5) {
                    decodeAlpha(in, start, alpha);
                }
                decodeColour(in, start + (dxt5 ? ALPHA_BLOCK_BYTES : 0), dxt5, dxt1Alpha, colour);
                for (int py = 0; py < BLOCK_SIZE; py++) {
                    for (int px = 0; px < BLOCK_SIZE; px++) {
                        int x = bx * BLOCK_SIZE + px;
                        int y = by * BLOCK_SIZE + py;
                        if (x < width && y < height) {
                            int texel = py * BLOCK_SIZE + px;
                            int argb = colour[texel];
                            int a = dxt5 ? alpha[texel] : argb >>> 24;
                            int o = (y * width + x) * packing.bytesPerPixel;
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >> 8) & 0xFF;
                            int b = argb & 0xFF;
                            switch (packing) {
                                case RGBA8 -> {
                                    out.put(o, (byte) r);
                                    out.put(o + 1, (byte) g);
                                    out.put(o + 2, (byte) b);
                                    out.put(o + 3, (byte) a);
                                }
                                case RGB565 -> out.putShort(o, (short) (((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3)));
                                case RGBA5551 -> out.putShort(o,
                                        (short) (((r >> 3) << 11) | ((g >> 3) << 6) | ((b >> 3) << 1) | (a >> 7)));
                                case RGBA4444 -> out.putShort(o,
                                        (short) (((r >> 4) << 12) | ((g >> 4) << 8) | ((b >> 4) << 4) | (a >> 4)));
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private static void decodeAlpha(ByteBuffer in, int start, int[] alpha) {
        int a0 = in.get(start) & 0xFF;
        int a1 = in.get(start + 1) & 0xFF;
        int[] palette = new int[8];
        palette[0] = a0;
        palette[1] = a1;
        if (a0 > a1) {
            for (int i = 1; i <= 6; i++) {
                palette[i + 1] = ((7 - i) * a0 + i * a1) / 7;
            }
        } else {
            for (int i = 1; i <= 4; i++) {
                palette[i + 1] = ((5 - i) * a0 + i * a1) / 5;
            }
            palette[6] = 0;
            palette[7] = OPAQUE;
        }
        long bits = 0;
        for (int i = 0; i < 6; i++) {
            bits |= (long) (in.get(start + 2 + i) & 0xFF) << (8 * i);
        }
        for (int i = 0; i < alpha.length; i++) {
            alpha[i] = palette[(int) (bits >> (3 * i)) & 7];
        }
    }

    private static void decodeColour(ByteBuffer in, int start, boolean dxt5, boolean dxt1Alpha, int[] colour) {
        int c0 = in.getShort(start) & 0xFFFF;
        int c1 = in.getShort(start + 2) & 0xFFFF;
        int[] palette = new int[4];
        palette[0] = expand565(c0);
        palette[1] = expand565(c1);
        if (dxt5 || c0 > c1) {
            palette[2] = mix(palette[0], 2, palette[1], 1, 3);
            palette[3] = mix(palette[0], 1, palette[1], 2, 3);
        } else {
            palette[2] = mix(palette[0], 1, palette[1], 1, 2);
            palette[3] = dxt1Alpha ? 0 : 0xFF000000;
        }
        int indices = in.getInt(start + 4);
        for (int i = 0; i < colour.length; i++) {
            colour[i] = palette[(indices >> (2 * i)) & 3];
        }
    }

    private static int expand565(int c) {
        int r = (c >> 11) & 0x1F;
        int g = (c >> 5) & 0x3F;
        int b = c & 0x1F;
        return 0xFF000000 | (((r << 3) | (r >> 2)) << 16) | (((g << 2) | (g >> 4)) << 8) | ((b << 3) | (b >> 2));
    }

    private static int mix(int a, int weightA, int b, int weightB, int divisor) {
        int r = (weightA * ((a >> 16) & 0xFF) + weightB * ((b >> 16) & 0xFF)) / divisor;
        int g = (weightA * ((a >> 8) & 0xFF) + weightB * ((b >> 8) & 0xFF)) / divisor;
        int bl = (weightA * (a & 0xFF) + weightB * (b & 0xFF)) / divisor;
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
