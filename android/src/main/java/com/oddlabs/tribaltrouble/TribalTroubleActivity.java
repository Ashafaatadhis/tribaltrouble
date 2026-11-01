package com.oddlabs.tribaltrouble;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
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

    private AndroidWindow window;
    private AndroidInputProvider input;
    private Thread engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams
                .LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        SurfaceView view = new SurfaceView(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.setOnApplyWindowInsetsListener(this::applySafeArea);
        setContentView(root);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        window = new AndroidWindow(this, view.getHolder(), metrics, getDisplay().getRefreshRate());
        input = new AndroidInputProvider(window);
        view.setOnTouchListener((_, event) -> input.onTouchEvent(event));

        System.setProperty("user.home", getFilesDir().getAbsolutePath());
        System.setProperty(ShaderDialect.ES_PROPERTY, "true");
        WindowSystems.install(new AndroidWindowSystem(window, input));
        engine = new Thread(this::runEngine, "engine");
        engine.start();
    }

    /**
     * Keeps the game out of the display cutout, the rounded corners and the side gesture zones, which would otherwise
     * cover the menu and the action buttons. The game runs in landscape with the system bars hidden, so the top and
     * bottom only need a thin margin and use the full height.
     */
    private WindowInsets applySafeArea(View root, WindowInsets insets) {
        Insets cutout = insets.getInsets(WindowInsets.Type.displayCutout());
        Insets gestures = insets.getInsets(WindowInsets.Type.systemGestures());
        float density = getResources().getDisplayMetrics().density;
        int sideMargin = Math.round(density * MIN_SIDE_MARGIN_DP);
        int edgeMargin = Math.round(density * MIN_TOP_BOTTOM_MARGIN_DP);
        root.setPadding(Math.max(Math.max(cutout.left, gestures.left), sideMargin), Math.max(cutout.top, edgeMargin),
                Math.max(Math.max(cutout.right, gestures.right), sideMargin), Math.max(cutout.bottom, edgeMargin));
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return input.onKeyEvent(event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return input.onKeyEvent(event) || super.onKeyUp(keyCode, event);
    }
}
