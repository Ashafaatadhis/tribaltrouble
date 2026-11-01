package com.oddlabs.tt;

import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Replaces the desktop {@code Main} in the Android build, which excludes {@code tt}'s own. It keeps the failure and
 * shutdown entry points the shared code calls; there are no dialogs or crash reports yet, so failures are only
 * logged. The activity starts the game instead of {@code main}.
 */
public final class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    /** Reports a failure the game keeps running after. */
    public static void fail(@NonNull Throwable t) {
        logger.log(Level.SEVERE, "Critical Failure", t);
    }

    /** Reports a failure the game exits after. */
    public static void failFatal(@NonNull Throwable t) {
        logger.log(Level.SEVERE, "Critical Failure", t);
    }

    public static void shutdown(int status) {
        Renderer.getRenderer().close();
        logger.info("Exiting");
        System.exit(status);
    }
}
