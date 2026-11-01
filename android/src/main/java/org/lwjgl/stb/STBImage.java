package org.lwjgl.stb;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** The part of stb_image the client uses, decoding with Android's {@link BitmapFactory}. */
public final class STBImage {
    private static final int RGBA_CHANNELS = 4;

    private static volatile String failureReason = "no error";

    private STBImage() {
    }

    /**
     * Decodes an image to straight-alpha RGBA bytes, like stb_image with four requested channels. Other channel
     * counts are not supported.
     *
     * @return the pixels, or null with {@link #stbi_failure_reason()} set
     */
    public static ByteBuffer stbi_load_from_memory(ByteBuffer buffer, IntBuffer x, IntBuffer y, IntBuffer channels,
            int desiredChannels) {
        if (desiredChannels != RGBA_CHANNELS) {
            throw new IllegalArgumentException("Only four channels are supported, not " + desiredChannels);
        }
        byte[] encoded = new byte[buffer.remaining()];
        buffer.duplicate().get(encoded);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPremultiplied = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.length, options);
        if (bitmap == null) {
            failureReason = "Cannot decode the image";
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        bitmap.recycle();

        ByteBuffer rgba = ByteBuffer.allocateDirect(argb.length * RGBA_CHANNELS);
        for (int pixel : argb) {
            rgba.put((byte) (pixel >> 16)).put((byte) (pixel >> 8)).put((byte) pixel).put((byte) (pixel >>> 24));
        }
        rgba.flip();

        x.put(0, width);
        y.put(0, height);
        channels.put(0, RGBA_CHANNELS);
        return rgba;
    }

    public static String stbi_failure_reason() {
        return failureReason;
    }

    /** The pixels are ordinary Java memory here, so there is nothing to free. */
    public static void stbi_image_free(ByteBuffer image) {
    }
}
