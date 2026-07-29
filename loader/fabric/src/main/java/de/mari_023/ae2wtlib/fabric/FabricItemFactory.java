package de.mari_023.ae2wtlib.fabric;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import appeng.core.definitions.ItemDefinition;
import appeng.core.registration.AERegistries;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.registration.Ae2wtlibItemFactory;

/**
 * Fabric implementation of {@link Ae2wtlibItemFactory}.
 * <p>
 * Our AE2 fork's {@code ItemDefinition} takes an {@code AEItemEntry} whose constructor is package-private, so the entry
 * has to come from {@code AERegistries.registerItem(...)}. That call also appends the entry to AE2's own pending list -
 * harmless <strong>because AE2 has already flushed it by the time we run</strong>:
 * {@code FabricRegistrar.registerAll()} is invoked from {@code AppEngFabric.init}, and AE2WTLib's registration is
 * driven from the entrypoint that runs strictly after it on both dists (see {@code AE2wtlibFabric} for the ordering
 * contract). The entry is then created, registered and bound here immediately, exactly like {@code FabricRegistrar}
 * does for AE2's own entries.
 */
public final class FabricItemFactory implements Ae2wtlibItemFactory {
    @Override
    public <T extends Item> ItemDefinition<T> item(String name, Function<Item.Properties, T> factory) {
        var id = AE2wtlibAPI.id(name);
        var entry = AERegistries.registerItem(id, factory);
        var item = entry.create();
        Registry.register(BuiltInRegistries.ITEM, id, item);
        // Bind immediately so definitions created later can already resolve this one (DeferredHolder semantics).
        entry.bind(item, BuiltInRegistries.ITEM.wrapAsHolder(item));
        return new ItemDefinition<>(name, entry);
    }
}
