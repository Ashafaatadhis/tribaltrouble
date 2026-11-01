package com.oddlabs.tt.window;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.render.SerializableDisplayMode;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Window over an Android surface. The engine loop blocks on its own thread, so this class owns the EGL context on
 * that thread instead of using GLSurfaceView's renderer callbacks. Surface callbacks arrive on the UI thread and are
 * only applied in {@link #pollEvents()}.
 */
public final class AndroidWindow implements Window, SurfaceHolder.Callback {
    private static final int GL_ES_MAJOR_VERSION = 3;
    private static final int GL_ES_MINOR_VERSION = 2;
    private static final int COLOR_BITS = 8;
    private static final int DEPTH_BITS = 24;
    private static final int DISPLAY_BITS_PER_PIXEL = 32;
    private static final float MM_PER_INCH = 25.4f;
    private static final long NO_SURFACE_SLEEP_MS = 50;

    private final @NonNull SurfaceHolder holder;
    private final @NonNull DisplayMetrics metrics;
    private final int refreshRate;
    private final Object surfaceLock = new Object();
    private final AtomicBoolean surfaceChanged = new AtomicBoolean();

    private volatile boolean surfaceValid;
    private volatile int surfaceWidth;
    private volatile int surfaceHeight;
    private volatile boolean active = true;
    private volatile boolean closeRequested;
    private boolean resized;

    private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig eglConfig;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;

    public AndroidWindow(@NonNull SurfaceHolder holder, @NonNull DisplayMetrics metrics, float refreshRate) {
        this.holder = holder;
        this.metrics = metrics;
        this.refreshRate = Math.round(refreshRate);
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceChanged(holder, 0, holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        synchronized (surfaceLock) {
            surfaceWidth = width;
            surfaceHeight = height;
            surfaceValid = true;
            surfaceChanged.set(true);
            surfaceLock.notifyAll();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        synchronized (surfaceLock) {
            surfaceValid = false;
            surfaceChanged.set(true);
        }
    }

    /** Called from the activity when it gains or loses focus; backs {@link #isActive()}. */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** The mode argument is ignored: the surface always fills the screen. */
    @Override
    public void create(@NonNull SerializableDisplayMode mode, boolean fullscreen) {
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            createContext();
        }
        synchronized (surfaceLock) {
            while (!surfaceValid) {
                try {
                    surfaceLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for the surface", e);
                }
            }
        }
        applySurfaceChange();
        GL.createCapabilities();
    }

    private void createContext() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw new IllegalStateException("eglInitialize failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }

        Settings settings = Settings.getSettings();
        int samples = settings != null ? settings.view_samples : 0;
        int[] configAttributes = {EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR, EGL14.EGL_RED_SIZE,
                COLOR_BITS, EGL14.EGL_GREEN_SIZE, COLOR_BITS, EGL14.EGL_BLUE_SIZE, COLOR_BITS, EGL14.EGL_ALPHA_SIZE,
                COLOR_BITS, EGL14.EGL_DEPTH_SIZE, DEPTH_BITS, EGL14.EGL_SAMPLE_BUFFERS, samples > 0 ? 1 : 0,
                EGL14.EGL_SAMPLES, samples, EGL14.EGL_NONE};
        EGLConfig[] configs = new EGLConfig[1];
        int[] configCount = new int[1];
        if (!EGL14.eglChooseConfig(eglDisplay, configAttributes, 0, configs, 0, 1, configCount, 0)
                || configCount[0] == 0) {
            throw new IllegalStateException("No EGL config with ES 3 and " + samples + " samples");
        }
        eglConfig = configs[0];

        int[] contextAttributes = {EGL14.EGL_CONTEXT_CLIENT_VERSION, GL_ES_MAJOR_VERSION,
                EGLExt.EGL_CONTEXT_MINOR_VERSION_KHR, GL_ES_MINOR_VERSION, EGL14.EGL_NONE};
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttributes, 0);
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw new IllegalStateException(
                    "OpenGL ES " + GL_ES_MAJOR_VERSION + "." + GL_ES_MINOR_VERSION + " is not available: 0x"
                            + Integer.toHexString(EGL14.eglGetError()));
        }
    }

    private void applySurfaceChange() {
        surfaceChanged.set(false);
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
            eglSurface = EGL14.EGL_NO_SURFACE;
        }
        if (surfaceValid) {
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, holder.getSurface(),
                    new int[]{EGL14.EGL_NONE}, 0);
            if (eglSurface == EGL14.EGL_NO_SURFACE || !EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface,
                    eglContext)) {
                throw new IllegalStateException("Cannot bind the EGL surface: 0x" + Integer.toHexString(
                        EGL14.eglGetError()));
            }
            resized = true;
        }
    }

    @Override
    public void close() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            return;
        }
        holder.removeCallback(this);
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
        }
        EGL14.eglDestroyContext(eglDisplay, eglContext);
        EGL14.eglTerminate(eglDisplay);
        eglSurface = EGL14.EGL_NO_SURFACE;
        eglContext = EGL14.EGL_NO_CONTEXT;
        eglDisplay = EGL14.EGL_NO_DISPLAY;
    }

    @Override
    public boolean isOpen() {
        return eglContext != EGL14.EGL_NO_CONTEXT;
    }

    /** Swaps, or idles while the app has no surface (backgrounded), so the loop does not spin. */
    @Override
    public void update() {
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglSwapBuffers(eglDisplay, eglSurface);
        } else {
            try {
                Thread.sleep(NO_SURFACE_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        pollEvents();
    }

    @Override
    public void pollEvents() {
        if (surfaceChanged.get()) {
            applySurfaceChange();
        }
    }

    @Override
    public boolean isCloseRequested() {
        boolean requested = closeRequested;
        closeRequested = false;
        return requested;
    }

    @Override
    public void setCloseRequested(boolean value) {
        closeRequested = value;
    }

    @Override
    public boolean isActive() {
        return active && surfaceValid;
    }

    @Override
    public boolean isVisible() {
        return surfaceValid;
    }

    @Override
    public boolean isIconified() {
        return !surfaceValid;
    }

    @Override
    public boolean wasResized() {
        boolean result = resized;
        resized = false;
        return result;
    }

    @Override
    public int getWidth() {
        return surfaceWidth;
    }

    @Override
    public int getHeight() {
        return surfaceHeight;
    }

    @Override
    public int getLogicalWidth() {
        return surfaceWidth;
    }

    @Override
    public int getLogicalHeight() {
        return surfaceHeight;
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public void setVSyncEnabled(boolean enabled) {
        EGL14.eglSwapInterval(eglDisplay, enabled ? 1 : 0);
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
    }

    @Override
    public @NonNull SerializableDisplayMode @NonNull [] getAvailableDisplayModes() {
        return new SerializableDisplayMode[]{getDisplayMode()};
    }

    @Override
    public @NonNull SerializableDisplayMode getDisplayMode() {
        return new SerializableDisplayMode(surfaceWidth, surfaceHeight, DISPLAY_BITS_PER_PIXEL, refreshRate);
    }

    @Override
    public void setDisplayMode(@NonNull SerializableDisplayMode mode) {
    }

    @Override
    public void setIcon(Path imagePath) {
    }

    @Override
    public void restore() {
    }

    @Override
    public void minimize() {
    }

    @Override
    public void show() {
    }

    @Override
    public void focus() {
    }

    @Override
    public void makeCurrent() {
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
    }

    @Override
    public boolean isFullscreen() {
        return true;
    }

    @Override
    public int[] getMonitorPhysicalSize() {
        return new int[]{Math.round(metrics.widthPixels / metrics.xdpi * MM_PER_INCH), Math.round(
                metrics.heightPixels / metrics.ydpi * MM_PER_INCH)};
    }

    @Override
    public float[] getMonitorContentScale() {
        return new float[]{metrics.density, metrics.density};
    }

    @Override
    public float[] getWindowContentScale() {
        return getMonitorContentScale();
    }
}
