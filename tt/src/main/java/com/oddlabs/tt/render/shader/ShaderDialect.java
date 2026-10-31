package com.oddlabs.tt.render.shader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapts the engine's desktop GLSL ({@code #version 410 core}) to GLSL ES 3.20 so the same shader
 * sources can run on OpenGL ES 3.2, which is what an Android port needs.
 *
 * <p>The adaptation is a no-op unless the system property {@value #ES_PROPERTY} is {@code true}, so
 * desktop behaviour is unchanged. When enabled it:
 *
 * <ul>
 *   <li>rewrites {@code #version 410 core} to {@code #version 320 es};
 *   <li>adds the {@code precision} statements that GLSL ES requires (a default float precision for
 *       fragment shaders, and a precision for every sampler type the shader uses);
 *   <li>rejects constructs that GLSL ES does not have (double precision types) with a clear error,
 *       instead of leaving the driver to report an obscure compile failure.
 * </ul>
 *
 * <p>Compiler error line numbers are offset by one when a precision line was inserted.
 */
public final class ShaderDialect {
    /** Set to {@code true} to compile shaders as GLSL ES 3.20 instead of desktop GLSL 4.10. */
    public static final String ES_PROPERTY = "com.oddlabs.tt.gl.es";

    public enum Stage {
        VERTEX,
        GEOMETRY,
        FRAGMENT
    }

    private static final Pattern ANY_VERSION = Pattern.compile("(?m)^[ \\t]*#version\\b.*$");
    private static final Pattern DESKTOP_VERSION =
            Pattern.compile("(?m)^[ \\t]*#version[ \\t]+410[ \\t]+core[ \\t]*$");
    private static final Pattern COMMENTS = Pattern.compile("(?s)/\\*.*?\\*/|//[^\\n]*");
    private static final Pattern DOUBLE_TYPES =
            Pattern.compile("\\b(double|dvec[234]|dmat[234](x[234])?)\\b");
    private static final String[] SAMPLER_TYPES = {
        "sampler2D", "sampler2DShadow", "samplerBuffer", "sampler2DArray", "sampler3D", "samplerCube"
    };

    private ShaderDialect() {}

    public static boolean isEsEnabled() {
        return Boolean.getBoolean(ES_PROPERTY);
    }

    /** Returns {@code source} unchanged on desktop, or its GLSL ES 3.20 form when ES mode is on. */
    public static String adapt(String source, Stage stage) {
        return isEsEnabled() ? toEs(source, stage) : source;
    }

    /** Converts desktop GLSL 4.10 core source to GLSL ES 3.20. Sources already in ES form pass through. */
    public static String toEs(String source, Stage stage) {
        Matcher version = ANY_VERSION.matcher(source);
        if (!version.find()) {
            throw new IllegalArgumentException("Shader has no #version directive");
        }
        String versionLine = version.group().trim();
        if (versionLine.endsWith(" es")) {
            return source;
        }
        if (!DESKTOP_VERSION.matcher(source).find()) {
            throw new IllegalArgumentException("Unsupported shader version for ES conversion: " + versionLine);
        }

        String code = COMMENTS.matcher(source).replaceAll("");
        Matcher doubles = DOUBLE_TYPES.matcher(code);
        if (doubles.find()) {
            throw new IllegalArgumentException(
                    "Type '" + doubles.group() + "' is not available in GLSL ES; rewrite the shader with float");
        }

        StringBuilder precision = new StringBuilder();
        if (stage == Stage.FRAGMENT) {
            precision.append("precision highp float; precision highp int;");
        }
        for (String type : SAMPLER_TYPES) {
            if (Pattern.compile("\\b" + type + "\\b").matcher(code).find()) {
                if (!precision.isEmpty()) {
                    precision.append(' ');
                }
                precision.append("precision highp ").append(type).append(';');
            }
        }

        StringBuilder out = new StringBuilder(source.length() + 160);
        out.append(source, 0, version.start()).append("#version 320 es");
        if (!precision.isEmpty()) {
            out.append('\n').append(precision);
        }
        out.append(source, version.end(), source.length());
        return out.toString();
    }
}
