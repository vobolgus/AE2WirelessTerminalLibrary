package de.mari_023.ae2wtlib.api;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Loader-neutral seam for the slot-indexed accessory inventory of a player (Curios on NeoForge, Trinkets on Fabric),
 * used by {@link de.mari_023.ae2wtlib.api.terminal.WUTHandler#findTerminal} so a terminal worn in an accessory slot is
 * found by the same funnel as one carried in the backpack.
 * <p>
 * The indices handed out here are the indices {@code appeng.menu.locator.MenuLocators#forCurioSlot(int)} is called
 * with, and AE2 writes them to the wire, so <strong>the implementation must be AE2's own view</strong> rather than a
 * second, independently ordered enumeration of the same slots — otherwise a locator minted here would resolve to a
 * different slot on AE2's side.
 * <p>
 * Nothing is injected on NeoForge: upstream's Curios support is disabled (see the FIXME in {@code WUTHandler}), and
 * with no implementation this reports "no accessory inventory", which is exactly the behaviour of the commented-out
 * block it replaces.
 */
public final class Ae2wtlibAccessories {
    /**
     * Slot-indexed read-only view; mirrors AE2's {@code CuriosSupport.Inventory}.
     */
    public interface Inventory {
        int size();

        /**
         * @return the <em>live</em> stack in that slot — menu hosts mutate it in place.
         */
        ItemStack getStack(int slot);
    }

    @FunctionalInterface
    public interface Lookup {
        @Nullable
        Inventory getAccessoryInventory(Player player);
    }

    @Nullable
    private static volatile Lookup lookup;

    private Ae2wtlibAccessories() {}

    /**
     * Injects the loader-specific lookup. Optional: a loader that has no accessory mod support simply never calls this.
     */
    public static void init(Lookup lookup) {
        Ae2wtlibAccessories.lookup = lookup;
    }

    /**
     * @return the player's accessory inventory, or null when no accessory mod is installed (or the loader has no
     *         support wired at all).
     */
    @Nullable
    public static Inventory get(Player player) {
        var current = lookup;
        return current == null ? null : current.getAccessoryInventory(player);
    }
}
