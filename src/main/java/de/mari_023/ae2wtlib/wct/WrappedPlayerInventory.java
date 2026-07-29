package de.mari_023.ae2wtlib.wct;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;

/**
 * PlayerInternalInventory returns the wrong size, so it doesn't work for the armor and offhand (what we actually care
 * about)
 * <p>
 * Was a record upstream; it is an <strong>abstract</strong> class here because
 * {@code InternalInventory#toResourceHandler()} is an abstract method in <em>upstream</em> AE2 (so the NeoForge build
 * must implement it) but does not exist at all in our Fabric fork, which replaced it with
 * {@code appeng.fabric.transfer.FabricResources} (PORTING_NOTES §3.2, seam #8). Each loader therefore contributes a
 * concrete subclass and installs it into {@link #factory}; all the actual inventory logic stays here, so an upstream
 * rebase only ever has to touch one copy.
 */
public abstract class WrappedPlayerInventory implements InternalInventory {
    /**
     * Loader hook: each entrypoint installs its concrete subclass here before any menu is opened.
     */
    @Nullable
    public static Function<Inventory, WrappedPlayerInventory> factory;

    private final Inventory playerInventory;

    protected WrappedPlayerInventory(Inventory playerInventory) {
        this.playerInventory = playerInventory;
    }

    public static WrappedPlayerInventory of(Inventory playerInventory) {
        var factory = WrappedPlayerInventory.factory;
        if (factory == null)
            throw new IllegalStateException("The loader-specific WrappedPlayerInventory factory is not installed");
        return factory.apply(playerInventory);
    }

    public Inventory playerInventory() {
        return playerInventory;
    }

    @Override
    public int size() {
        return playerInventory.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        return switch (slotIndex) {
            case 36, 37, 38, 39, Inventory.SLOT_OFFHAND -> playerInventory.getItem(slotIndex);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItemDirect(int slotIndex, ItemStack stack) {
        playerInventory.setItem(slotIndex, stack);
    }
}
