package de.mari_023.ae2wtlib.fabric.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;

import de.mari_023.ae2wtlib.config.Ae2wtlibConfigStore;

/**
 * {@link Ae2wtlibConfigStore} backed by night-config TOML files, reading and writing the same
 * {@code config/ae2wtlib.toml} / {@code config/ae2wtlib-client.toml} files (same value paths, types and defaults) that
 * NeoForge's {@code ModConfigSpec} produces. Trimmed copy of AE2's {@code appeng.fabric.config.FabricConfigStore}
 * (proven code - only the value types AE2WTLib actually uses were kept).
 * <p>
 * <strong>Behaviour note (vs. NeoForge):</strong> there is no file watcher; config changes on disk are only picked up
 * at startup (the {@code onLoadOrReload} listeners fire once after the initial load). In-game changes via
 * {@link #save()} work as on NeoForge.
 */
public final class FabricConfigStore implements Ae2wtlibConfigStore {
    private static final Logger LOG = LoggerFactory.getLogger(FabricConfigStore.class);

    private final List<String> currentSection = new ArrayList<>();
    private final List<Definition<?>> definitions = new ArrayList<>();
    private final List<Runnable> loadListeners = new ArrayList<>();
    private final List<SectionComment> sectionComments = new ArrayList<>();
    @Nullable
    private String pendingComment;
    @Nullable
    private CommentedFileConfig config;

    /**
     * Creates a store, lets the given config define its structure into it and loads the backing file from the loader's
     * config directory.
     */
    public static <T> T create(String fileName, Function<Ae2wtlibConfigStore, T> definer) {
        var store = new FabricConfigStore();
        var config = definer.apply(store);
        store.load(FabricLoader.getInstance().getConfigDir().resolve(fileName));
        return config;
    }

    private void load(Path file) {
        var config = CommentedFileConfig.builder(file).sync().build();
        config.load();

        for (var definition : definitions) {
            definition.correct(config);
            config.setComment(definition.path, definition.buildComment());
        }
        for (var sectionComment : sectionComments) {
            config.setComment(sectionComment.path, sectionComment.comment);
        }

        this.config = config;
        // Always write back once so defaults/comments materialize in fresh files.
        config.save();

        for (var listener : loadListeners) {
            listener.run();
        }
    }

    @Override
    public void comment(String comment) {
        this.pendingComment = comment;
    }

    @Override
    public void push(String section) {
        currentSection.add(section);
        if (pendingComment != null) {
            sectionComments.add(new SectionComment(String.join(".", currentSection), pendingComment));
            pendingComment = null;
        }
    }

    @Override
    public void pop() {
        currentSection.removeLast();
    }

    @Override
    public Value<Boolean> defineBoolean(String name, boolean defaultValue) {
        return define(new Definition<>(path(name), defaultValue, null,
                raw -> raw instanceof Boolean b ? b : null,
                value -> value));
    }

    @Override
    public Value<Double> defineDouble(String name, double defaultValue) {
        return define(new Definition<>(path(name), defaultValue, null,
                raw -> raw instanceof Number number ? number.doubleValue() : null,
                value -> value));
    }

    @Override
    public void save() {
        if (config != null) {
            config.save();
        }
    }

    @Override
    public void onLoadOrReload(Runnable listener) {
        loadListeners.add(listener);
    }

    private String path(String name) {
        if (currentSection.isEmpty()) {
            return name;
        }
        return String.join(".", currentSection) + "." + name;
    }

    private <T> Value<T> define(Definition<T> definition) {
        definition.comment = pendingComment;
        pendingComment = null;
        definitions.add(definition);
        return new ValueImpl<>(definition);
    }

    private record SectionComment(String path, String comment) {
    }

    private static final class Definition<T> {
        final String path;
        final T defaultValue;
        @Nullable
        final String typeComment;
        final Function<@Nullable Object, @Nullable T> deserializer;
        final Function<T, Object> serializer;
        @Nullable
        String comment;

        Definition(String path, T defaultValue, @Nullable String typeComment,
                Function<@Nullable Object, @Nullable T> deserializer, Function<T, Object> serializer) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.typeComment = typeComment;
            this.deserializer = deserializer;
            this.serializer = serializer;
        }

        /**
         * Ensures the config contains a valid value at this path, replacing invalid/missing entries with the default
         * (mirroring ModConfigSpec's correction behaviour).
         */
        void correct(CommentedConfig config) {
            var raw = config.get(path);
            if (deserializer.apply(raw) == null) {
                if (raw != null) {
                    LOG.warn("Correcting invalid config value {} for {} back to default {}", raw, path, defaultValue);
                }
                config.set(path, serializer.apply(defaultValue));
            }
        }

        @Nullable
        String buildComment() {
            if (comment != null && typeComment != null) {
                return comment + "\n" + typeComment;
            } else if (comment != null) {
                return comment;
            } else {
                return typeComment;
            }
        }
    }

    private final class ValueImpl<T> implements Value<T> {
        private final Definition<T> definition;

        ValueImpl(Definition<T> definition) {
            this.definition = definition;
        }

        @Override
        public T get() {
            var config = FabricConfigStore.this.config;
            if (config == null) {
                return definition.defaultValue;
            }
            var value = definition.deserializer.apply(config.get(definition.path));
            return value != null ? value : definition.defaultValue;
        }

        @Override
        public void set(T value) {
            var config = FabricConfigStore.this.config;
            if (config != null) {
                config.set(definition.path, definition.serializer.apply(value));
            }
        }
    }
}
