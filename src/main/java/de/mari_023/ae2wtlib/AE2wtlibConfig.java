package de.mari_023.ae2wtlib;

import org.jspecify.annotations.Nullable;

import de.mari_023.ae2wtlib.config.Ae2wtlibConfigStore;

/**
 * The common (server-side) config, backed by a loader-specific {@link Ae2wtlibConfigStore}.
 * <p>
 * Value paths, types and defaults are unchanged from the {@code ModConfigSpec} version, so the generated
 * {@code config/ae2wtlib.toml} is identical and existing config files keep working.
 */
public record AE2wtlibConfig(Ae2wtlibConfigStore.Value<Double> magnetCardRangeValue) {
    public static final String FILE_NAME = "ae2wtlib.toml";

    @Nullable
    private static volatile AE2wtlibConfig instance;

    /**
     * Defines this config into the given store. Called once per launch by the loader entrypoint.
     */
    public static AE2wtlibConfig register(Ae2wtlibConfigStore store) {
        var config = new AE2wtlibConfig(store.defineDouble("magnet_card_range", 16.0));
        instance = config;
        return config;
    }

    public static AE2wtlibConfig config() {
        var config = instance;
        if (config == null)
            throw new IllegalStateException("AE2wtlib's common config has not been registered yet");
        return config;
    }

    public double magnetCardRange() {
        return magnetCardRangeValue().get();
    }
}
