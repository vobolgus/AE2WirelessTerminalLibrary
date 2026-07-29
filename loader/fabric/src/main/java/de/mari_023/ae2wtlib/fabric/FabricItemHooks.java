package de.mari_023.ae2wtlib.fabric;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import de.mari_023.ae2wtlib.Ae2wtlibItemHooks;

/**
 * Fabric implementation of {@link Ae2wtlibItemHooks}: NeoForge's defaults, expressed in vanilla terms. See the
 * interface for the per-method rationale and the one behaviour gap ({@code PreventRemoteMovement}).
 */
public final class FabricItemHooks implements Ae2wtlibItemHooks {
    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int slot) {
        return false;
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, Player player) {
        return player.getEquipmentSlotForItem(stack) == slot;
    }

    @Override
    public boolean preventsRemoteMovement(ItemEntity entity) {
        return false;
    }
}
