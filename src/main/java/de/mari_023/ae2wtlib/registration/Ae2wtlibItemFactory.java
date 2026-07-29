package de.mari_023.ae2wtlib.registration;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.Item;

import appeng.core.definitions.ItemDefinition;

/**
 * Loader-neutral seam for creating this mod's {@link ItemDefinition}s.
 * <p>
 * <strong>Why this seam exists (risk R1 in PORTING_NOTES §6).</strong> {@code ItemDefinition} is an <em>AE2</em> class,
 * and its constructor differs between the two AE2 builds this repository compiles against:
 * <ul>
 * <li>upstream AE2 26.1.10-beta (what {@code :neoforge} resolves):
 * {@code ItemDefinition(String, DeferredItem<T>)},</li>
 * <li>our de-NeoForged AE2 Fabric fork (what {@code :loader:fabric} resolves):
 * {@code ItemDefinition(String, AEItemEntry<T>)}.</li>
 * </ul>
 * The <em>type</em> {@code ItemDefinition<T>} is identical on both sides, so every consumer in the shared tree stays
 * untouched — only the construction is per-loader, which is exactly what this factory abstracts.
 * <p>
 * Must be injected before {@code AE2wtlibItems} is class-loaded, because that class builds all definitions in its
 * static initializer. Both entrypoints therefore call {@link #init} first and {@code AE2wtlibItems.init()} second.
 */
public interface Ae2wtlibItemFactory {
    /**
     * Mirrors NeoForge's {@code DeferredRegister.Items#registerItem}: the factory receives an {@link Item.Properties}
     * that already carries the item's id (golden rule #7 — {@code Properties.setId} before construction).
     *
     * @param name the path of the item id inside the {@code ae2wtlib} namespace; also the definition's english name
     */
    <T extends Item> ItemDefinition<T> item(String name, Function<Item.Properties, T> factory);

    static Ae2wtlibItemFactory get() {
        var instance = Holder.INSTANCE;
        if (instance == null)
            throw new IllegalStateException("The loader-specific Ae2wtlibItemFactory has not been initialized yet");
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction, before
     * {@code AE2wtlibItems} is touched.
     */
    static void init(Ae2wtlibItemFactory factory) {
        if (Holder.INSTANCE != null)
            throw new IllegalStateException("Ae2wtlibItemFactory has already been initialized");
        Holder.INSTANCE = factory;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile Ae2wtlibItemFactory INSTANCE;

        private Holder() {}
    }
}
