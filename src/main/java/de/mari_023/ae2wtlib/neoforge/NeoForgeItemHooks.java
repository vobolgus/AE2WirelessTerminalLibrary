package de.mari_023.ae2wtlib.neoforge;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import de.mari_023.ae2wtlib.Ae2wtlibItemHooks;

/**
 * NeoForge implementation of {@link Ae2wtlibItemHooks} - each method is the extension-method call that used to sit
 * inline at the call site, so upstream behaviour is unchanged.
 */
public final class NeoForgeItemHooks implements Ae2wtlibItemHooks {
    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int slot) {
        return stack.isNotReplaceableByPickAction(player, slot);
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, Player player) {
        return stack.canEquip(slot, player);
    }

    @Override
    public boolean preventsRemoteMovement(ItemEntity entity) {
        return entity.getPersistentData().contains("PreventRemoteMovement");
    }
}
