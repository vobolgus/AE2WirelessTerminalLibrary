package de.mari_023.ae2wtlib.fabric;

import net.minecraft.world.entity.player.Inventory;

import de.mari_023.ae2wtlib.wct.WrappedPlayerInventory;

/**
 * Fabric concrete subclass of {@link WrappedPlayerInventory}. Empty on purpose: the only loader-specific member of the
 * upstream record was {@code toResourceHandler()}, which our AE2 Fabric fork removed from {@code InternalInventory}
 * (PORTING_NOTES §3.2). W4 revisits whether anything on Fabric needs the equivalent
 * {@code appeng.fabric.transfer.FabricResources} view (seam #8).
 */
public final class FabricWrappedPlayerInventory extends WrappedPlayerInventory {
    public FabricWrappedPlayerInventory(Inventory playerInventory) {
        super(playerInventory);
    }
}
