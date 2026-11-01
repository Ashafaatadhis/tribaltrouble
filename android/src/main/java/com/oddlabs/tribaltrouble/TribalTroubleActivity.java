package com.oddlabs.tribaltrouble;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import com.oddlabs.tt.Main;
import com.oddlabs.tt.input.AndroidInputProvider;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.window.AndroidWindow;
import com.oddlabs.tt.window.AndroidWindowSystem;
import com.oddlabs.tt.window.WindowSystems;

public final class TribalTroubleActivity extends Activity {
    private AndroidWindow window;
    private AndroidInputProvider input;
    private Thread engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SurfaceView view = new SurfaceView(this);
        setContentView(view);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        window = new AndroidWindow(this, view.getHolder(), metrics, getDisplay().getRefreshRate());
        input = new AndroidInputProvider(window);
        view.setOnTouchListener((_, event) -> input.onTouchEvent(event));

        System.setProperty("user.home", getFilesDir().getAbsolutePath());
        WindowSystems.install(new AndroidWindowSystem(window, input));
        engine = new Thread(this::runEngine, "engine");
        engine.start();
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
