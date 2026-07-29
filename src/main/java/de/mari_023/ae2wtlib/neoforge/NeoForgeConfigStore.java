package de.mari_023.ae2wtlib.neoforge;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import de.mari_023.ae2wtlib.config.Ae2wtlibConfigStore;

/**
 * NeoForge implementation of {@link Ae2wtlibConfigStore}, a thin adapter over {@code ModConfigSpec.Builder}. Because
 * every define maps 1:1 onto the builder call upstream used, the generated TOML is byte-identical to before.
 */
public final class NeoForgeConfigStore implements Ae2wtlibConfigStore {
    private final ModConfigSpec.Builder builder;

    private NeoForgeConfigStore(ModConfigSpec.Builder builder) {
        this.builder = builder;
    }

    /**
     * Builds the config of type {@code T} and registers its spec with the given mod container.
     *
     * @param definer the config's {@code register(store)} method
     */
    public static <T> T register(ModContainer modContainer, ModConfig.Type type, String fileName,
            Function<Ae2wtlibConfigStore, T> definer) {
        Pair<T, ModConfigSpec> pair = new ModConfigSpec.Builder()
                .configure(builder -> definer.apply(new NeoForgeConfigStore(builder)));
        modContainer.registerConfig(type, pair.getRight(), fileName);
        return pair.getLeft();
    }

    @Override
    public void comment(String comment) {
        builder.comment(comment);
    }

    @Override
    public void push(String section) {
        builder.push(section);
    }

    @Override
    public void pop() {
        builder.pop();
    }

    @Override
    public Value<Boolean> defineBoolean(String name, boolean defaultValue) {
        return wrap(builder.define(name, defaultValue));
    }

    @Override
    public Value<Double> defineDouble(String name, double defaultValue) {
        return wrap(builder.define(name, defaultValue));
    }

    @Override
    public void save() {
        // ModConfigSpec.ConfigValue#set already writes through to the loaded config.
    }

    @Override
    public void onLoadOrReload(Runnable listener) {
        // NeoForge fires ModConfigEvent.Loading/Reloading; AE2WTLib has no listener today, so this is a no-op.
    }

    private static <T> Value<T> wrap(ModConfigSpec.ConfigValue<T> value) {
        return new Value<>() {
            @Override
            public T get() {
                return value.get();
            }

            @Override
            public void set(T newValue) {
                value.set(newValue);
            }
        };
    }
}
