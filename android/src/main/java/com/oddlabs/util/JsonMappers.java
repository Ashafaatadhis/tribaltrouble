package com.oddlabs.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.jspecify.annotations.NonNull;

/**
 * Replaces the desktop class in the Android build. ART does not report records as records, so Jackson's record
 * support does not apply; this mapper builds them through their constructor parameter names and reads their fields.
 */
public final class JsonMappers {
    private JsonMappers() {
    }

    public static @NonNull ObjectMapper create() {
        return new ObjectMapper().registerModule(new ParameterNamesModule()).setVisibility(PropertyAccessor.FIELD,
                JsonAutoDetect.Visibility.ANY);
    }
}
