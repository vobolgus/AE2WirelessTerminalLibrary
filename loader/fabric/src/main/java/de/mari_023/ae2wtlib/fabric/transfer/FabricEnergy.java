package de.mari_023.ae2wtlib.fabric.transfer;

import team.reborn.energy.api.EnergyStorage;

import appeng.fabric.transfer.PoweredItemEnergyStorage;

import de.mari_023.ae2wtlib.AE2wtlibItems;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;

/**
 * Seam #7 — the Fabric half of NeoForge's {@code RegisterCapabilitiesEvent} block in {@code AE2wtlibForge} (W4).
 *
 * <h2>Mapping</h2>
 *
 * <pre>
 * NeoForge: event.registerItem(Capabilities.Energy.ITEM,
 *         (_, context) -&gt; new PoweredItemCapabilities(context, item, item), item);
 * Fabric: EnergyStorage.ITEM.registerForItems(
 *         (stack, context) -&gt; new PoweredItemEnergyStorage(context, item, item), item);
 * </pre>
 *
 * {@code appeng.items.tools.powered.powersink.PoweredItemCapabilities} is a NeoForge-overlay class in our AE2 fork and
 * has no Fabric counterpart under that name (PORTING_NOTES §3.2); the fork's Fabric twin is
 * {@code appeng.fabric.transfer.PoweredItemEnergyStorage}, which exposes AE2's chargeable items over Team Reborn Energy
 * on top of the Fabric Transfer API. Same three-argument shape ({@code ContainerItemContext}, the valid
 * {@link net.minecraft.world.item.Item}, the {@code IAEItemPowerStorage}), and {@link ItemWT} is both of the latter two
 * — exactly as on NeoForge.
 * <p>
 * The call is copied from {@code appeng.fabric.init.InitApiLookup#registerPowerStorageItem}, which is how AE2 registers
 * its own powered items.
 *
 * <h2>Which items, and why not the crafting terminal</h2>
 *
 * The same three as NeoForge: universal, pattern access, pattern encoding. The wireless <em>crafting</em> terminal is
 * deliberately absent on both loaders — this mod does not register that item ({@code AEItemsMixin} swaps AE2's own
 * factory for {@code ItemWCT}), so AE2 itself already registers the energy capability for it via
 * {@code registerPowerStorageItem(AEItems.WIRELESS_CRAFTING_TERMINAL)}, and that definition resolves to our
 * {@code ItemWCT}.
 *
 * <h2>Ordering</h2>
 *
 * Playbook Part 8: duplicate {@code ItemApiLookup} providers are resolved <strong>first registration wins</strong>.
 * This runs from {@code AE2wtlibFabric.init()}, i.e. at the TAIL of {@code AppEngFabric#init}, strictly after AE2's own
 * {@code InitApiLookup}. The item sets are disjoint, so there is nothing to shadow either way — but the ordering is the
 * safe one regardless (AE2 registers its items first, we add ours).
 */
public final class FabricEnergy {
    private FabricEnergy() {}

    public static void registerPowerStorageItems() {
        registerPowerStorageItem(AE2wtlibItems.UNIVERSAL_TERMINAL.asItem());
        registerPowerStorageItem(AE2wtlibItems.PATTERN_ACCESS_TERMINAL.asItem());
        registerPowerStorageItem(AE2wtlibItems.PATTERN_ENCODING_TERMINAL.asItem());
    }

    private static void registerPowerStorageItem(ItemWT item) {
        EnergyStorage.ITEM.registerForItems((stack, context) -> new PoweredItemEnergyStorage(context, item, item),
                item);
    }
}
