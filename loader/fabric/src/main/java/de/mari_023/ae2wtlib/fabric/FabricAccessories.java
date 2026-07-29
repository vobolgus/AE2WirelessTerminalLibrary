package de.mari_023.ae2wtlib.fabric;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.integration.modules.curios.CuriosSupport;

import de.mari_023.ae2wtlib.api.Ae2wtlibAccessories;

/**
 * Fabric implementation of the {@link Ae2wtlibAccessories} seam: a thin adapter onto AE2's {@code CuriosSupport}, which
 * on Fabric is backed by Trinkets ({@code appeng.fabric.integration.trinkets}).
 * <p>
 * <strong>Deliberately not a second Trinkets integration.</strong> The slot index this view hands out is passed to
 * {@code MenuLocators.forCurioSlot(int)} and AE2 resolves it through its <em>own</em> flattening of the trinket slots
 * (and sends it over the wire); enumerating the slots independently here would risk a different order and therefore a
 * locator that points at the wrong slot. Riding AE2's view makes the two agree by construction — and means this module
 * needs no Trinkets dependency at all, only the slot tag that opts our terminals in.
 * <p>
 * When Trinkets is absent AE2's seam returns null and this reports "no accessory inventory", so the whole path stays
 * dormant.
 */
public final class FabricAccessories implements Ae2wtlibAccessories.Lookup {
    @Override
    public Ae2wtlibAccessories.@Nullable Inventory getAccessoryInventory(Player player) {
        var curios = CuriosSupport.get().getCuriosInventory(player);
        if (curios == null) {
            return null;
        }
        return new Ae2wtlibAccessories.Inventory() {
            @Override
            public int size() {
                return curios.size();
            }

            @Override
            public ItemStack getStack(int slot) {
                return curios.getStack(slot);
            }
        };
    }
}
