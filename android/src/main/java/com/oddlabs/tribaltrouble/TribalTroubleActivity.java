package com.oddlabs.tribaltrouble;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.window.OnBackInvokedDispatcher;
import com.oddlabs.tt.Main;
import com.oddlabs.tt.input.AndroidInputProvider;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.shader.ShaderDialect;
import com.oddlabs.tt.window.AndroidWindow;
import com.oddlabs.tt.window.AndroidWindowSystem;
import com.oddlabs.tt.window.WindowSystems;

public final class TribalTroubleActivity extends Activity {
    private static final int MIN_SIDE_MARGIN_DP = 24;
    private static final int MIN_TOP_BOTTOM_MARGIN_DP = 6;

    /** The game surface, which also takes typed text from the on-screen keyboard and passes it to the engine. */
    private static final class GameSurfaceView extends SurfaceView {
        private AndroidInputProvider input;

        GameSurfaceView(Context context) {
            super(context);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }

        void bind(AndroidInputProvider input) {
            this.input = input;
        }

        @Override
        public boolean onCheckIsTextEditor() {
            return true;
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
            outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI
                    | EditorInfo.IME_FLAG_NO_FULLSCREEN;
            return new BaseInputConnection(this, false) {
                @Override
                public boolean commitText(CharSequence text, int newCursorPosition) {
                    input.typeText(text);
                    return true;
                }

                @Override
                public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                    for (int i = 0; i < beforeLength; i++) {
                        input.pressBackspace();
                    }
                    return true;
                }

                @Override
                public boolean sendKeyEvent(KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                            input.pressBackspace();
                        } else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            input.pressEnter();
                        } else if (event.getUnicodeChar() > 0) {
                            input.typeText(String.valueOf((char) event.getUnicodeChar()));
                        }
                    }
                    return true;
                }

                @Override
                public boolean performEditorAction(int editorAction) {
                    input.pressEnter();
                    return true;
                }
            };
        }
    }

    private GameSurfaceView view;
    private AndroidWindow window;
    private AndroidInputProvider input;
    private Thread engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams
                .LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        view = new GameSurfaceView(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.setOnApplyWindowInsetsListener(this::applySafeArea);
        setContentView(root);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        window = new AndroidWindow(this, view.getHolder(), metrics, getDisplay().getRefreshRate());
        input = new AndroidInputProvider(window);
        view.bind(input);
        view.setOnTouchListener((_, event) -> input.onTouchEvent(event));
        input.setKeyboardListener(wanted -> runOnUiThread(() -> setKeyboardVisible(wanted)));
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                input::pressBack);

        System.setProperty("user.home", getFilesDir().getAbsolutePath());
        System.setProperty(ShaderDialect.ES_PROPERTY, "true");
        WindowSystems.install(new AndroidWindowSystem(window, input));
        engine = new Thread(this::runEngine, "engine");
        engine.start();
    }

    private void setKeyboardVisible(boolean visible) {
        InputMethodManager keyboard = getSystemService(InputMethodManager.class);
        if (visible) {
            view.requestFocus();
            keyboard.showSoftInput(view, 0);
        } else {
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Keeps the game out of the display cutout, the rounded corners and the side gesture zones, which would otherwise
     * cover the menu and the action buttons. The game runs in landscape with the system bars hidden, so the top and
     * bottom only need a thin margin and use the full height; the bottom also rises above the on-screen keyboard.
     */
    private WindowInsets applySafeArea(View root, WindowInsets insets) {
        Insets cutout = insets.getInsets(WindowInsets.Type.displayCutout());
        Insets gestures = insets.getInsets(WindowInsets.Type.systemGestures());
        Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
        float density = getResources().getDisplayMetrics().density;
        int sideMargin = Math.round(density * MIN_SIDE_MARGIN_DP);
        int edgeMargin = Math.round(density * MIN_TOP_BOTTOM_MARGIN_DP);
        root.setPadding(Math.max(Math.max(cutout.left, gestures.left), sideMargin), Math.max(cutout.top, edgeMargin),
                Math.max(Math.max(cutout.right, gestures.right), sideMargin),
                Math.max(Math.max(cutout.bottom, keyboard.bottom), edgeMargin));
        return WindowInsets.CONSUMED;
    }

    private void runEngine() {
        try {
            Renderer.getRenderer().run();
        } catch (Throwable t) {
            Main.failFatal(t);
        } finally {
            Main.shutdown(0);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        window.setActive(hasFocus);
        if (hasFocus) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    /** Back reaches the game through the back callback, so the key itself must not be sent a second time. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || input.onKeyEvent(event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || input.onKeyEvent(event) || super.onKeyUp(keyCode, event);
    }
}
