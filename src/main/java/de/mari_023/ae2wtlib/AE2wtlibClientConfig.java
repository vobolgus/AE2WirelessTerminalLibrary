package de.mari_023.ae2wtlib;

import org.jspecify.annotations.Nullable;

import de.mari_023.ae2wtlib.config.Ae2wtlibConfigStore;

/**
 * The client config, backed by a loader-specific {@link Ae2wtlibConfigStore}.
 * <p>
 * Value paths, types and defaults are unchanged from the {@code ModConfigSpec} version, so the generated
 * {@code config/ae2wtlib-client.toml} is identical and existing config files keep working.
 */
public record AE2wtlibClientConfig(Ae2wtlibConfigStore.Value<Boolean> alwaysShowTerminalSelectorValue) {
    public static final String FILE_NAME = "ae2wtlib-client.toml";

    @Nullable
    private static volatile AE2wtlibClientConfig instance;

    /**
     * Defines this config into the given store. Called once per launch by the loader's client entrypoint.
     */
    public static AE2wtlibClientConfig register(Ae2wtlibConfigStore store) {
        var config = new AE2wtlibClientConfig(store.defineBoolean("always_show_terminal_selector", false));
        instance = config;
        return config;
    }

    public static AE2wtlibClientConfig config() {
        var config = instance;
        if (config == null)
            throw new IllegalStateException("AE2wtlib's client config has not been registered yet");
        return config;
    }

    public boolean alwaysShowTerminalSelector() {
        return alwaysShowTerminalSelectorValue().get();
    }
}
