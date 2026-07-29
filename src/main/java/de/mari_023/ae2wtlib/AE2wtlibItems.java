package de.mari_023.ae2wtlib;

import java.util.function.Function;

import net.minecraft.world.item.Item;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import appeng.items.tools.powered.WirelessTerminalItem;

import de.mari_023.ae2wtlib.api.terminal.ItemWUT;
import de.mari_023.ae2wtlib.registration.Ae2wtlibItemFactory;
import de.mari_023.ae2wtlib.wat.ItemWAT;
import de.mari_023.ae2wtlib.wet.ItemWET;

public final class AE2wtlibItems {
    private AE2wtlibItems() {}

    // was: public static final DeferredRegister.Items DR = DeferredRegister.createItems(MOD_NAME);
    // The DeferredRegister moved into the NeoForge overlay (de.mari_023.ae2wtlib.neoforge.NeoForgeItemFactory).
    // The declaration order below is unchanged and is what determines the items' raw registry ids on BOTH loaders -
    // do not reorder (playbook Part 10).

    public static final ItemDefinition<WirelessTerminalItem> WIRELESS_CRAFTING_TERMINAL = AEItems.WIRELESS_CRAFTING_TERMINAL;
    public static final ItemDefinition<ItemWET> PATTERN_ENCODING_TERMINAL = item("wireless_pattern_encoding_terminal",
            ItemWET::new);
    public static final ItemDefinition<ItemWAT> PATTERN_ACCESS_TERMINAL = item("wireless_pattern_access_terminal",
            ItemWAT::new);
    public static final ItemDefinition<ItemWUT> UNIVERSAL_TERMINAL = item("wireless_universal_terminal", ItemWUT::new);

    public static final ItemDefinition<Item> QUANTUM_BRIDGE_CARD = item("quantum_bridge_card",
            p -> Upgrades.createUpgradeCardItem(p.stacksTo(1)));
    public static final ItemDefinition<Item> MAGNET_CARD = item("magnet_card",
            p -> Upgrades.createUpgradeCardItem(p.stacksTo(1)));

    /**
     * Forces this class - and with it every {@code item(...)} call above - to be loaded. On NeoForge the class load
     * used to be a side effect of {@code AE2wtlibItems.DR.register(modEventBus)}; both loaders now do it explicitly.
     */
    @SuppressWarnings("EmptyMethod")
    public static void init() {}

    private static <T extends Item> ItemDefinition<T> item(String name, Function<Item.Properties, T> factory) {
        return Ae2wtlibItemFactory.get().item(name, factory);
    }
}
