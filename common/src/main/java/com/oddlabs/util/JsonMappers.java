package com.oddlabs.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;

/** Creates the mappers for the game's own JSON files. The Android build replaces this class. */
public final class JsonMappers {
    private JsonMappers() {
    }

    public static @NonNull ObjectMapper create() {
        return new ObjectMapper();
    }
}
