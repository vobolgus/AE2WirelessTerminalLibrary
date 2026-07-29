package de.mari_023.ae2wtlib.neoforge;

import java.util.function.Function;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.core.definitions.ItemDefinition;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.registration.Ae2wtlibItemFactory;

/**
 * NeoForge implementation of {@link Ae2wtlibItemFactory}: unchanged upstream behaviour, i.e. a
 * {@code DeferredRegister.Items} plus upstream AE2's {@code ItemDefinition(String, DeferredItem)} constructor.
 */
public final class NeoForgeItemFactory implements Ae2wtlibItemFactory {
    public static final DeferredRegister.Items DR = DeferredRegister.createItems(AE2wtlibAPI.MOD_NAME);

    @Override
    public <T extends Item> ItemDefinition<T> item(String name, Function<Item.Properties, T> factory) {
        return new ItemDefinition<>(name, DR.registerItem(name, factory));
    }

    public static void register(IEventBus modEventBus) {
        DR.register(modEventBus);
    }
}
