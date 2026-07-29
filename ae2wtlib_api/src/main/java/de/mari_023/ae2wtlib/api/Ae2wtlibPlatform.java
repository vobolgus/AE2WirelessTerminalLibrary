package de.mari_023.ae2wtlib.api;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * Loader-neutral seam for the handful of mod-loader queries the API module makes, replacing NeoForge's
 * {@code ModList.get().isLoaded(...)}.
 * <p>
 * Deliberately a {@link Predicate} holder rather than a full interface: the only query is "is mod X loaded", and it has
 * to answer <em>before</em> anything else is initialized ({@code AE2wtlibAPIImpl}'s static initializer uses it to
 * decide whether to install the no-op API implementation for standalone use of the API jar). The loader entrypoints
 * inject their implementation via {@link #init}; if nothing was injected, the fallback answers {@code false}, which
 * reproduces the standalone-API behaviour.
 */
public final class Ae2wtlibPlatform {
    @Nullable
    private static volatile Predicate<String> modLoadedCheck;

    private Ae2wtlibPlatform() {}

    /**
     * Injects the loader-specific mod-presence check. Must be called before any other AE2WTLib class is touched, i.e.
     * as the very first statement of the loader entrypoint.
     */
    public static void init(Predicate<String> check) {
        modLoadedCheck = check;
    }

    public static boolean isModLoaded(String modId) {
        var check = modLoadedCheck;
        return check != null && check.test(modId);
    }
}
