package de.mari_023.ae2wtlib.config;

/**
 * Loader-neutral seam for the backing store of {@link de.mari_023.ae2wtlib.AE2wtlibConfig} and
 * {@link de.mari_023.ae2wtlib.AE2wtlibClientConfig}, replacing NeoForge's {@code ModConfigSpec}.
 * <p>
 * Copied in shape from AE2's {@code appeng.core.config.ConfigStore} (which does not exist in <em>upstream</em> AE2, so
 * it cannot simply be reused here — see PORTING_NOTES §3.2). The definition methods mirror the builder semantics of
 * {@code ModConfigSpec.Builder}: a comment applies to the next defined value or section, and sections nest via
 * push/pop.
 * <p>
 * Deliberately minimal — only the two value types AE2WTLib actually defines are present, so that the Fabric
 * implementation has no untested surface. Adding a type means adding it in three places: here, in
 * {@code NeoForgeConfigStore} and in {@code FabricConfigStore}.
 */
public interface Ae2wtlibConfigStore {
    /**
     * Sets the comment to attach to the next defined value or pushed section.
     */
    void comment(String comment);

    /**
     * Enters a (possibly new) section with the given name.
     */
    void push(String section);

    /**
     * Leaves the current section.
     */
    void pop();

    Value<Boolean> defineBoolean(String name, boolean defaultValue);

    /**
     * Unrestricted double, i.e. NeoForge's {@code builder.define(name, defaultValue)} — deliberately <em>not</em> a
     * ranged define, so the generated TOML carries no "Range:" comment and stays byte-identical to what upstream's
     * {@code ModConfigSpec} writes.
     */
    Value<Double> defineDouble(String name, double defaultValue);

    /**
     * Persists the current values to disk.
     */
    void save();

    /**
     * Registers a listener that is invoked whenever this store's values are (re-)loaded from disk.
     */
    void onLoadOrReload(Runnable listener);

    /**
     * Typed handle to a single config value.
     */
    interface Value<T> {
        T get();

        void set(T value);
    }
}
