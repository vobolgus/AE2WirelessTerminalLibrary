package de.mari_023.ae2wtlib.fabric;

import net.minecraft.world.entity.player.Inventory;

import de.mari_023.ae2wtlib.wct.WrappedPlayerInventory;

/**
 * Fabric concrete subclass of {@link WrappedPlayerInventory} — seam #8.
 *
 * <h2>W4 verdict: deliberately empty, and that is the faithful answer</h2>
 *
 * The only loader-specific member of the upstream record was {@code toResourceHandler()}, which our AE2 Fabric fork
 * removed from {@code InternalInventory} (PORTING_NOTES §3.2). There is <strong>no per-inventory override point on
 * Fabric at all</strong>: the fork replaced the virtual method with the static dispatcher
 * {@code appeng.fabric.transfer.FabricResources#toStorage(InternalInventory)}, which switches on the concrete inventory
 * type ({@code SupplierInternalInventory}, {@code StorageInternalInventory}, {@code PlayerInternalInventory},
 * {@code CarriedItemInventory}, {@code CombinedInternalInventory}, {@code BaseInternalInventory}) and otherwise falls
 * back to a generic {@code InternalInventoryStorage} adapter. A third-party {@code InternalInventory} cannot substitute
 * its own view.
 *
 * <h2>Why that costs nothing here (call-site analysis, W4)</h2>
 *
 * {@code WrappedPlayerInventory} has exactly <strong>one</strong> consumer in this mod:
 * {@code de.mari_023.ae2wtlib.wct.ArmorSlot}, which hands it to {@code AppEngSlot} as a menu-slot backing. Menu slots
 * never reach the transfer API. On the AE2 side, every caller of {@code FabricResources#toStorage} — and of its
 * NeoForge twin {@code NeoForgeResources#toResourceHandler} — is a block-entity/part API-lookup registration in
 * {@code InitApiLookup} / {@code InitCapabilityProviders}, never a menu inventory. Upstream's
 * {@code toResourceHandler()} override is therefore a defensive dead branch, and its absence on Fabric is unobservable.
 * <p>
 * ⚠ Recorded for completeness: <em>if</em> something ever did route this inventory through the Fabric transfer API, the
 * generic adapter would expose only the armour + offhand view that {@code getStackInSlot} returns, whereas NeoForge's
 * {@code PlayerInventoryWrapper.of(playerInventory())} exposes the full player inventory. Closing that gap would need a
 * change in the <em>AE2 fork</em> (a recognised inventory type, or an extension hook in
 * {@code FabricResources#toStorage}), not here.
 */
public final class FabricWrappedPlayerInventory extends WrappedPlayerInventory {
    public FabricWrappedPlayerInventory(Inventory playerInventory) {
        super(playerInventory);
    }
}
