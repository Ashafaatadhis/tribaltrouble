package com.oddlabs.net;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

/**
 * Opens object streams that only deserialize classes matching a filter pattern such as {@code "com.oddlabs.**;!*"}.
 * The Android build replaces this class, because {@link ObjectInputFilter} is not in the Android SDK.
 */
public final class DeserializationFilter {
    private final @NonNull ObjectInputFilter filter;

    private DeserializationFilter(@NonNull ObjectInputFilter filter) {
        this.filter = filter;
    }

    public static @NonNull DeserializationFilter of(@NonNull String pattern) {
        return new DeserializationFilter(ObjectInputFilter.Config.createFilter(pattern));
    }

    public @NonNull ObjectInputStream open(@NonNull InputStream in) throws IOException {
        ObjectInputStream stream = new ObjectInputStream(in);
        stream.setObjectInputFilter(filter);
        return stream;
    }
}
