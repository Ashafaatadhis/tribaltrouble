package com.oddlabs.tt.input;

import android.view.KeyEvent;
import android.view.MotionEvent;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.window.AndroidWindow;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Input from an Android view. The UI thread only records raw touches and keys; the engine thread turns the touches
 * into mouse and key events when it polls, so it can ask the GUI what is under a finger.
 *
 * <ul>
 * <li>On a control (button, slider, dialog) one finger is the left mouse button.
 * <li>On the 3D view one finger drags the camera, a tap is a click, and holding still before moving starts a
 * selection box.
 * <li>Two fingers pinch to zoom and drag to pan; three fingers toggle the status display.
 * </ul>
 */
public final class AndroidInputProvider implements InputProvider<Object> {
    private static final int BUTTON_NONE = -1;
    private static final int BUTTON_LEFT = 0;
    private static final int WHEEL_STEP = 120;
    private static final float PINCH_PIXELS_PER_STEP = 48f;
    private static final float PAN_START_PIXELS = 3f;
    private static final float TAP_SLOP_PIXELS = 40f;
    private static final long PAN_IDLE_NANOS = 150_000_000L;
    private static final long LONG_PRESS_NANOS = 400_000_000L;
    private static final int STATUS_FINGERS = 3;

    private enum Mode {
        IDLE, PENDING_WORLD, PANNING, SELECTING, CONTROL_DRAG, MULTI_TOUCH
    }

    private record RawTouch(int action, int pointerCount, float x0, float y0, float x1, float y1, long nanos) {
    }

    private record QueuedKey(int key, boolean state, boolean repeat, int mods, char character) {
    }

    private record QueuedMouse(int button, boolean state, int x, int y, int dWheel) {
    }

    private final @NonNull AndroidWindow window;
    private volatile @Nullable GUIRoot guiRoot;
    private volatile @Nullable Consumer<Boolean> keyboardListener;
    private boolean keyboardWanted;

    private final ConcurrentLinkedQueue<@NonNull RawTouch> rawTouches = new ConcurrentLinkedQueue<>();

    private final Deque<@NonNull QueuedKey> keyEvents = new ArrayDeque<>();
    private @Nullable QueuedKey currentKeyEvent;
    private final Set<Integer> keysDown = new HashSet<>();

    private final Deque<@NonNull QueuedMouse> mouseEvents = new ArrayDeque<>();
    private @Nullable QueuedMouse currentMouseEvent;
    private volatile int mouseX;
    private volatile int mouseY;
    private volatile boolean leftDown;
    private volatile boolean grabbed;

    // Gesture state, only touched on the engine thread.
    private Mode mode = Mode.IDLE;
    private float downX;
    private float downY;
    private long downNanos;
    private float lastX;
    private float lastY;
    private float lastPinchDistance;
    private float pinchRemainder;
    private float lastCentroidX;
    private float lastCentroidY;
    private long lastPanNanos;
    private int heldHorizontalKey;
    private int heldVerticalKey;

    public AndroidInputProvider(@NonNull AndroidWindow window) {
        this.window = window;
    }

    @Override
    public void setGuiRoot(@NonNull GUIRoot root) {
        this.guiRoot = root;
    }

    /** Called on the UI thread; touch coordinates are top-left based pixels. */
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int count = event.getPointerCount();
        rawTouches.add(new RawTouch(event.getActionMasked(), count, event.getX(0), event.getY(0),
                count > 1 ? event.getX(1) : 0, count > 1 ? event.getY(1) : 0, System.nanoTime()));
        return true;
    }

    @Override
    public void pollMouse() {
        RawTouch touch;
        while ((touch = rawTouches.poll()) != null) {
            process(touch);
        }
        long now = System.nanoTime();
        if (mode == Mode.PENDING_WORLD && now - downNanos >= LONG_PRESS_NANOS) {
            mode = Mode.SELECTING;
            moveTo(downX, downY);
            setLeft(true);
        }
        releasePanKeysWhenIdle(now);
    }

    private void process(@NonNull RawTouch touch) {
        switch (touch.action()) {
            case MotionEvent.ACTION_DOWN -> {
                downX = touch.x0();
                downY = touch.y0();
                downNanos = touch.nanos();
                updateKeyboard(touch.x0(), touch.y0());
                if (isOverWorld(touch.x0(), touch.y0())) {
                    mode = Mode.PENDING_WORLD;
                } else {
                    mode = Mode.CONTROL_DRAG;
                    moveTo(touch.x0(), touch.y0());
                    setLeft(true);
                }
            }
            case MotionEvent.ACTION_MOVE -> move(touch);
            case MotionEvent.ACTION_POINTER_DOWN -> pointerDown(touch);
            case MotionEvent.ACTION_POINTER_UP -> setPanKeys(0, 0);
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                boolean up = touch.action() == MotionEvent.ACTION_UP;
                switch (mode) {
                    case PENDING_WORLD -> {
                        if (up) {
                            moveTo(downX, downY);
                            setLeft(true);
                            setLeft(false);
                        }
                    }
                    case SELECTING, CONTROL_DRAG -> {
                        moveTo(touch.x0(), touch.y0());
                        setLeft(false);
                    }
                    default -> {
                    }
                }
                setPanKeys(0, 0);
                mode = Mode.IDLE;
            }
            default -> {
            }
        }
    }

    private void move(@NonNull RawTouch touch) {
        if (touch.pointerCount() >= 2) {
            if (mode == Mode.MULTI_TOUCH) {
                pinch(touch);
                panWithTwoFingers(touch);
            }
            return;
        }
        switch (mode) {
            case CONTROL_DRAG, SELECTING -> moveTo(touch.x0(), touch.y0());
            case PENDING_WORLD -> {
                if (Math.hypot(touch.x0() - downX, touch.y0() - downY) > TAP_SLOP_PIXELS) {
                    mode = Mode.PANNING;
                    lastX = touch.x0();
                    lastY = touch.y0();
                }
            }
            case PANNING -> {
                panCamera(touch.x0() - lastX, touch.y0() - lastY);
                lastX = touch.x0();
                lastY = touch.y0();
            }
            default -> {
            }
        }
    }

    private void pointerDown(@NonNull RawTouch touch) {
        if (touch.pointerCount() >= STATUS_FINGERS) {
            toggleStatus();
            return;
        }
        if (leftDown) {
            setLeft(false);
        }
        mode = Mode.MULTI_TOUCH;
        lastPinchDistance = (float) Math.hypot(touch.x0() - touch.x1(), touch.y0() - touch.y1());
        pinchRemainder = 0;
        lastCentroidX = (touch.x0() + touch.x1()) / 2;
        lastCentroidY = (touch.y0() + touch.y1()) / 2;
    }

    /** Asks for the on-screen keyboard when a touch lands on a text field, and for it to go away anywhere else. */
    private void updateKeyboard(float touchX, float touchY) {
        GUIRoot root = guiRoot;
        boolean wanted = root != null && root.isTextFieldAt((int) touchX, window.getHeight() - (int) touchY - 1);
        if (wanted != keyboardWanted) {
            keyboardWanted = wanted;
            Consumer<Boolean> listener = keyboardListener;
            if (listener != null) {
                listener.accept(wanted);
            }
        }
    }

    /** Called when the on-screen keyboard is wanted ({@code true}) or no longer wanted. */
    public void setKeyboardListener(@Nullable Consumer<Boolean> listener) {
        keyboardListener = listener;
    }

    /** Types text from the on-screen keyboard; the engine reads each character as typed input. */
    public void typeText(@NonNull CharSequence text) {
        synchronized (keyEvents) {
            for (int i = 0; i < text.length(); i++) {
                keyEvents.add(new QueuedKey(0, true, false, 0, text.charAt(i)));
            }
        }
    }

    /** Presses and releases one key, given as a GLFW key code. */
    public void pressKey(int glfwKey) {
        synchronized (keyEvents) {
            keyEvents.add(new QueuedKey(glfwKey, true, false, 0, (char) 0));
            keyEvents.add(new QueuedKey(glfwKey, false, false, 0, (char) 0));
        }
    }

    /** The Back button and gesture open the game's menu, as Escape does on a keyboard. */
    public void pressBack() {
        pressKey(AndroidKeys.GLFW_KEY_ESCAPE);
    }

    public void pressBackspace() {
        pressKey(AndroidKeys.GLFW_KEY_BACKSPACE);
    }

    public void pressEnter() {
        pressKey(AndroidKeys.GLFW_KEY_ENTER);
    }

    private boolean isOverWorld(float touchX, float touchY) {
        GUIRoot root = guiRoot;
        return root != null && root.isOverWorld((int) touchX, window.getHeight() - (int) touchY - 1);
    }

    /** The engine wants a bottom-left origin. */
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

    private void pinch(@NonNull RawTouch touch) {
        float distance = (float) Math.hypot(touch.x0() - touch.x1(), touch.y0() - touch.y1());
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

    private void panWithTwoFingers(@NonNull RawTouch touch) {
        float centroidX = (touch.x0() + touch.x1()) / 2;
        float centroidY = (touch.y0() + touch.y1()) / 2;
        panCamera(centroidX - lastCentroidX, centroidY - lastCentroidY);
        lastCentroidX = centroidX;
        lastCentroidY = centroidY;
    }

    /**
     * Pans the camera by holding an arrow key, since that is how the camera takes panning. The world follows the
     * fingers, so the camera moves the opposite way.
     */
    private void panCamera(float dx, float dy) {
        if (Math.abs(dx) < PAN_START_PIXELS && Math.abs(dy) < PAN_START_PIXELS) {
            return;
        }
        lastPanNanos = System.nanoTime();
        int horizontal = Math.abs(dx) < PAN_START_PIXELS ? 0 : dx > 0 ? AndroidKeys.GLFW_KEY_LEFT
                : AndroidKeys.GLFW_KEY_RIGHT;
        int vertical = Math.abs(dy) < PAN_START_PIXELS ? 0 : dy > 0 ? AndroidKeys.GLFW_KEY_UP
                : AndroidKeys.GLFW_KEY_DOWN;
        setPanKeys(horizontal, vertical);
    }

    /** Lets go of the panning keys once the fingers stop moving, since no event arrives when they hold still. */
    private void releasePanKeysWhenIdle(long now) {
        if ((heldHorizontalKey != 0 || heldVerticalKey != 0) && now - lastPanNanos > PAN_IDLE_NANOS) {
            setPanKeys(0, 0);
        }
    }

    /** Holds the given arrow keys and releases the ones held before; 0 means no key on that axis. */
    private void setPanKeys(int horizontal, int vertical) {
        synchronized (keyEvents) {
            switchPanKey(heldHorizontalKey, horizontal);
            switchPanKey(heldVerticalKey, vertical);
            heldHorizontalKey = horizontal;
            heldVerticalKey = vertical;
        }
    }

    private void switchPanKey(int held, int wanted) {
        if (held == wanted) {
            return;
        }
        if (held != 0) {
            keysDown.remove(held);
            keyEvents.add(new QueuedKey(held, false, false, 0, (char) 0));
        }
        if (wanted != 0) {
            keysDown.add(wanted);
            keyEvents.add(new QueuedKey(wanted, true, false, 0, (char) 0));
        }
    }

    /** Presses Control+I, which shows the frame rate and other status lines. */
    private void toggleStatus() {
        synchronized (keyEvents) {
            keyEvents.add(new QueuedKey(AndroidKeys.GLFW_KEY_LEFT_CONTROL, true, false, AndroidKeys.GLFW_MOD_CONTROL,
                    (char) 0));
            keyEvents.add(new QueuedKey(AndroidKeys.GLFW_KEY_A + ('I' - 'A'), true, false,
                    AndroidKeys.GLFW_MOD_CONTROL, (char) 0));
            keyEvents.add(new QueuedKey(AndroidKeys.GLFW_KEY_A + ('I' - 'A'), false, false,
                    AndroidKeys.GLFW_MOD_CONTROL, (char) 0));
            keyEvents.add(new QueuedKey(AndroidKeys.GLFW_KEY_LEFT_CONTROL, false, false, 0, (char) 0));
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
