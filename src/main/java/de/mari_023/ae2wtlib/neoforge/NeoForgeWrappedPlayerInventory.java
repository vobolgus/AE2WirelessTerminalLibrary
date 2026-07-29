package de.mari_023.ae2wtlib.neoforge;

import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;

import de.mari_023.ae2wtlib.wct.WrappedPlayerInventory;

/**
 * Adds back the {@code toResourceHandler()} override that upstream {@code WrappedPlayerInventory} carried.
 * {@code InternalInventory#toResourceHandler} only exists in <em>upstream</em> AE2; our Fabric fork removed it
 * (PORTING_NOTES §3.2), which is why it cannot live in the shared class.
 */
public final class NeoForgeWrappedPlayerInventory extends WrappedPlayerInventory {
    public NeoForgeWrappedPlayerInventory(Inventory playerInventory) {
        super(playerInventory);
    }

    @Override
    public ResourceHandler<ItemResource> toResourceHandler() {
        return PlayerInventoryWrapper.of(playerInventory());
    }
}
