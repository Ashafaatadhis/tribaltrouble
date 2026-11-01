package com.oddlabs.tt.input;

import android.view.KeyEvent;

/** Translates Android key codes to the GLFW key codes the engine's {@code Key} enum is built on. */
final class AndroidKeys {
    static final int GLFW_MOD_SHIFT = 0x1;
    static final int GLFW_MOD_CONTROL = 0x2;
    static final int GLFW_MOD_ALT = 0x4;
    static final int GLFW_MOD_SUPER = 0x8;

    private static final int GLFW_UNKNOWN = 0;
    private static final int GLFW_KEY_SPACE = 32;
    private static final int GLFW_KEY_COMMA = 44;
    private static final int GLFW_KEY_MINUS = 45;
    private static final int GLFW_KEY_PERIOD = 46;
    private static final int GLFW_KEY_SLASH = 47;
    private static final int GLFW_KEY_0 = 48;
    private static final int GLFW_KEY_SEMICOLON = 59;
    private static final int GLFW_KEY_EQUAL = 61;
    private static final int GLFW_KEY_A = 65;
    private static final int GLFW_KEY_LEFT_BRACKET = 91;
    private static final int GLFW_KEY_BACKSLASH = 92;
    private static final int GLFW_KEY_RIGHT_BRACKET = 93;
    private static final int GLFW_KEY_GRAVE_ACCENT = 96;
    private static final int GLFW_KEY_ESCAPE = 256;
    private static final int GLFW_KEY_ENTER = 257;
    private static final int GLFW_KEY_TAB = 258;
    private static final int GLFW_KEY_BACKSPACE = 259;
    private static final int GLFW_KEY_INSERT = 260;
    private static final int GLFW_KEY_DELETE = 261;
    private static final int GLFW_KEY_RIGHT = 262;
    private static final int GLFW_KEY_LEFT = 263;
    private static final int GLFW_KEY_DOWN = 264;
    private static final int GLFW_KEY_UP = 265;
    private static final int GLFW_KEY_PAGE_UP = 266;
    private static final int GLFW_KEY_PAGE_DOWN = 267;
    private static final int GLFW_KEY_HOME = 268;
    private static final int GLFW_KEY_END = 269;
    private static final int GLFW_KEY_F1 = 290;
    private static final int GLFW_KEY_LEFT_SHIFT = 340;
    private static final int GLFW_KEY_LEFT_CONTROL = 341;
    private static final int GLFW_KEY_LEFT_ALT = 342;
    private static final int GLFW_KEY_RIGHT_SHIFT = 344;
    private static final int GLFW_KEY_RIGHT_CONTROL = 345;
    private static final int GLFW_KEY_RIGHT_ALT = 346;

    private AndroidKeys() {
    }

    /** {@return the GLFW key code, or 0 when the key has no equivalent} */
    static int toGlfw(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            return GLFW_KEY_A + (keyCode - KeyEvent.KEYCODE_A);
        }
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            return GLFW_KEY_0 + (keyCode - KeyEvent.KEYCODE_0);
        }
        if (keyCode >= KeyEvent.KEYCODE_F1 && keyCode <= KeyEvent.KEYCODE_F12) {
            return GLFW_KEY_F1 + (keyCode - KeyEvent.KEYCODE_F1);
        }
        return switch (keyCode) {
            case KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> GLFW_KEY_ESCAPE;
            case KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> GLFW_KEY_ENTER;
            case KeyEvent.KEYCODE_TAB -> GLFW_KEY_TAB;
            case KeyEvent.KEYCODE_DEL -> GLFW_KEY_BACKSPACE;
            case KeyEvent.KEYCODE_FORWARD_DEL -> GLFW_KEY_DELETE;
            case KeyEvent.KEYCODE_INSERT -> GLFW_KEY_INSERT;
            case KeyEvent.KEYCODE_SPACE -> GLFW_KEY_SPACE;
            case KeyEvent.KEYCODE_DPAD_RIGHT -> GLFW_KEY_RIGHT;
            case KeyEvent.KEYCODE_DPAD_LEFT -> GLFW_KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_DOWN -> GLFW_KEY_DOWN;
            case KeyEvent.KEYCODE_DPAD_UP -> GLFW_KEY_UP;
            case KeyEvent.KEYCODE_PAGE_UP -> GLFW_KEY_PAGE_UP;
            case KeyEvent.KEYCODE_PAGE_DOWN -> GLFW_KEY_PAGE_DOWN;
            case KeyEvent.KEYCODE_MOVE_HOME -> GLFW_KEY_HOME;
            case KeyEvent.KEYCODE_MOVE_END -> GLFW_KEY_END;
            case KeyEvent.KEYCODE_COMMA -> GLFW_KEY_COMMA;
            case KeyEvent.KEYCODE_PERIOD -> GLFW_KEY_PERIOD;
            case KeyEvent.KEYCODE_MINUS -> GLFW_KEY_MINUS;
            case KeyEvent.KEYCODE_EQUALS -> GLFW_KEY_EQUAL;
            case KeyEvent.KEYCODE_SLASH -> GLFW_KEY_SLASH;
            case KeyEvent.KEYCODE_SEMICOLON -> GLFW_KEY_SEMICOLON;
            case KeyEvent.KEYCODE_LEFT_BRACKET -> GLFW_KEY_LEFT_BRACKET;
            case KeyEvent.KEYCODE_RIGHT_BRACKET -> GLFW_KEY_RIGHT_BRACKET;
            case KeyEvent.KEYCODE_BACKSLASH -> GLFW_KEY_BACKSLASH;
            case KeyEvent.KEYCODE_GRAVE -> GLFW_KEY_GRAVE_ACCENT;
            case KeyEvent.KEYCODE_SHIFT_LEFT -> GLFW_KEY_LEFT_SHIFT;
            case KeyEvent.KEYCODE_SHIFT_RIGHT -> GLFW_KEY_RIGHT_SHIFT;
            case KeyEvent.KEYCODE_CTRL_LEFT -> GLFW_KEY_LEFT_CONTROL;
            case KeyEvent.KEYCODE_CTRL_RIGHT -> GLFW_KEY_RIGHT_CONTROL;
            case KeyEvent.KEYCODE_ALT_LEFT -> GLFW_KEY_LEFT_ALT;
            case KeyEvent.KEYCODE_ALT_RIGHT -> GLFW_KEY_RIGHT_ALT;
            default -> GLFW_UNKNOWN;
        };
    }

    static int toGlfwMods(int metaState) {
        int mods = 0;
        if ((metaState & KeyEvent.META_SHIFT_ON) != 0) {
            mods |= GLFW_MOD_SHIFT;
        }
        if ((metaState & KeyEvent.META_CTRL_ON) != 0) {
            mods |= GLFW_MOD_CONTROL;
        }
        if ((metaState & KeyEvent.META_ALT_ON) != 0) {
            mods |= GLFW_MOD_ALT;
        }
        if ((metaState & KeyEvent.META_META_ON) != 0) {
            mods |= GLFW_MOD_SUPER;
        }
        return mods;
    }
}
