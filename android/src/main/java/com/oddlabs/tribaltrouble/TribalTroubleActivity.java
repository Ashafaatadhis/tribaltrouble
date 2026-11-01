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
import android.widget.FrameLayout;
import com.oddlabs.tt.Main;
import com.oddlabs.tt.input.AndroidInputProvider;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.shader.ShaderDialect;
import com.oddlabs.tt.window.AndroidWindow;
import com.oddlabs.tt.window.AndroidWindowSystem;
import com.oddlabs.tt.window.WindowSystems;

public final class TribalTroubleActivity extends Activity {
    private static final int MIN_EDGE_MARGIN_DP = 24;

    private AndroidWindow window;
    private AndroidInputProvider input;
    private Thread engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
     * Keeps the game out of the display cutout, the rounded corners and the edge gesture zones, which would otherwise
     * cover the menu and the action buttons at the screen edges.
     */
    private WindowInsets applySafeArea(View root, WindowInsets insets) {
        Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                | WindowInsets.Type.systemGestures());
        int margin = Math.round(getResources().getDisplayMetrics().density * MIN_EDGE_MARGIN_DP);
        root.setPadding(Math.max(safe.left, margin), Math.max(safe.top, margin), Math.max(safe.right, margin),
                Math.max(safe.bottom, margin));
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
