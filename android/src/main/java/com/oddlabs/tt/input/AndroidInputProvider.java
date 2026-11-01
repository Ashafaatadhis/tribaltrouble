package com.oddlabs.tt.input;

import android.view.KeyEvent;
import android.view.MotionEvent;
import com.oddlabs.tt.window.AndroidWindow;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Input from an Android view. One finger acts as the left mouse button; a second finger turns the gesture into a
 * pinch that is reported as the mouse wheel. The UI thread feeds {@link #onTouchEvent} and {@link #onKeyEvent}; the
 * engine thread drains the queues through the {@link InputProvider} methods.
 */
public final class AndroidInputProvider implements InputProvider<Object> {
    private static final int BUTTON_NONE = -1;
    private static final int BUTTON_LEFT = 0;
    private static final int WHEEL_STEP = 120;
    private static final float PINCH_PIXELS_PER_STEP = 48f;

    private final @NonNull AndroidWindow window;

    private final Deque<@NonNull QueuedKey> keyEvents = new ArrayDeque<>();
    private @Nullable QueuedKey currentKeyEvent;
    private final Set<Integer> keysDown = new HashSet<>();

    private final Deque<@NonNull QueuedMouse> mouseEvents = new ArrayDeque<>();
    private @Nullable QueuedMouse currentMouseEvent;
    private volatile int mouseX;
    private volatile int mouseY;
    private volatile boolean leftDown;
    private volatile boolean grabbed;

    private boolean pinching;
    private float lastPinchDistance;
    private float pinchRemainder;

    private record QueuedKey(int key, boolean state, boolean repeat, int mods, char character) {
    }

    private record QueuedMouse(int button, boolean state, int x, int y, int dWheel) {
    }

    public AndroidInputProvider(@NonNull AndroidWindow window) {
        this.window = window;
    }

    /** Touch coordinates are top-left based pixels; the engine wants a bottom-left origin. */
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                pinching = false;
                moveTo(event.getX(), event.getY());
                setLeft(true);
            }
            case MotionEvent.ACTION_MOVE -> {
                if (event.getPointerCount() >= 2) {
                    pinch(event);
                } else if (!pinching) {
                    moveTo(event.getX(), event.getY());
                }
            }
            case MotionEvent.ACTION_POINTER_DOWN -> {
                setLeft(false);
                pinching = true;
                lastPinchDistance = pinchDistance(event);
                pinchRemainder = 0;
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!pinching) {
                    moveTo(event.getX(), event.getY());
                }
                setLeft(false);
                pinching = false;
            }
            default -> {
            }
        }
        return true;
    }

    private void moveTo(float touchX, float touchY) {
        mouseX = (int) touchX;
        mouseY = window.getHeight() - (int) touchY - 1;
        synchronized (mouseEvents) {
            mouseEvents.add(new QueuedMouse(BUTTON_NONE, false, mouseX, mouseY, 0));
        }
    }

    private void setLeft(boolean down) {
        if (leftDown == down) {
            return;
        }
        leftDown = down;
        synchronized (mouseEvents) {
            mouseEvents.add(new QueuedMouse(BUTTON_LEFT, down, mouseX, mouseY, 0));
        }
    }

    private static float pinchDistance(MotionEvent event) {
        return (float) Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
    }

    private void pinch(MotionEvent event) {
        float distance = pinchDistance(event);
        pinchRemainder += (distance - lastPinchDistance) / PINCH_PIXELS_PER_STEP;
        lastPinchDistance = distance;
        int steps = (int) pinchRemainder;
        if (steps != 0) {
            pinchRemainder -= steps;
            synchronized (mouseEvents) {
                mouseEvents.add(new QueuedMouse(BUTTON_NONE, false, mouseX, mouseY, steps * WHEEL_STEP));
            }
        }
    }

    /** {@return true if the key was translated and queued} */
    public boolean onKeyEvent(@NonNull KeyEvent event) {
        int key = AndroidKeys.toGlfw(event.getKeyCode());
        char character = 0;
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (down) {
            int unicode = event.getUnicodeChar();
            if (unicode > 0 && !Character.isISOControl(unicode)) {
                character = (char) unicode;
            }
        }
        if (key == 0 && character == 0) {
            return false;
        }
        int mods = AndroidKeys.toGlfwMods(event.getMetaState());
        synchronized (keyEvents) {
            if (key != 0) {
                if (down) {
                    keysDown.add(key);
                } else {
                    keysDown.remove(key);
                }
            }
            keyEvents.add(new QueuedKey(key, down, down && event.getRepeatCount() > 0, mods, character));
        }
        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public void pollKeyboard() {
    }

    @Override
    public boolean nextKeyboardEvent() {
        synchronized (keyEvents) {
            currentKeyEvent = keyEvents.poll();
            return currentKeyEvent != null;
        }
    }

    @Override
    public int getEventKey() {
        return currentKeyEvent != null ? currentKeyEvent.key() : 0;
    }

    @Override
    public boolean getEventKeyState() {
        return currentKeyEvent != null && currentKeyEvent.state();
    }

    @Override
    public int getEventKeyMods() {
        return currentKeyEvent != null ? currentKeyEvent.mods() : 0;
    }

    @Override
    public char getEventCharacter() {
        return currentKeyEvent != null ? currentKeyEvent.character() : 0;
    }

    @Override
    public boolean isRepeatEvent() {
        return currentKeyEvent != null && currentKeyEvent.repeat();
    }

    @Override
    public boolean isKeyDown(int keyCode) {
        synchronized (keyEvents) {
            return keysDown.contains(keyCode);
        }
    }

    @Override
    public void pollMouse() {
    }

    @Override
    public boolean nextMouseEvent() {
        synchronized (mouseEvents) {
            currentMouseEvent = mouseEvents.poll();
            return currentMouseEvent != null;
        }
    }

    @Override
    public int getEventButton() {
        return currentMouseEvent != null ? currentMouseEvent.button() : BUTTON_NONE;
    }

    @Override
    public boolean getEventButtonState() {
        return currentMouseEvent != null && currentMouseEvent.state();
    }

    @Override
    public int getEventDWheel() {
        return currentMouseEvent != null ? currentMouseEvent.dWheel() : 0;
    }

    @Override
    public int getEventX() {
        return currentMouseEvent != null ? currentMouseEvent.x() : mouseX;
    }

    @Override
    public int getEventY() {
        return currentMouseEvent != null ? currentMouseEvent.y() : mouseY;
    }

    @Override
    public int getMouseX() {
        return mouseX;
    }

    @Override
    public int getMouseY() {
        return mouseY;
    }

    @Override
    public boolean isButtonDown(int button) {
        return button == BUTTON_LEFT && leftDown;
    }

    /** A touch screen has no cursor to warp, so this is ignored. */
    @Override
    public void setCursorPosition(int x, int y) {
    }

    @Override
    public void setGrabbed(boolean grabbed) {
        this.grabbed = grabbed;
    }

    @Override
    public boolean isGrabbed() {
        return grabbed;
    }

    @Override
    public boolean isCursorInWindow() {
        return true;
    }

    @Override
    public void setNativeCursor(@Nullable Object cursor) {
    }
}
