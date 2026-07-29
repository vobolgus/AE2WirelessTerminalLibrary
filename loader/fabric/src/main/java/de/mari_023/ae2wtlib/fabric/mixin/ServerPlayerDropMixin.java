package de.mari_023.ae2wtlib.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;

import de.mari_023.ae2wtlib.AE2wtlibEvents;

/**
 * Fabric-only twin of the shared {@code de.mari_023.ae2wtlib.mixin.ServerPlayerMixin} — restock the hotbar slot after
 * dropping from it.
 *
 * <h2>⚠ Why this is a separate class (R4, the headline W3 finding)</h2>
 *
 * The shared mixin captures {@code @Local(name = "selected") ItemStack} out of {@code ServerPlayer#drop(boolean)}. That
 * local <strong>does not exist in vanilla</strong> — it is created by a NeoForge patch. javap on both jars, 2026-07-29:
 *
 * <pre>
 * vanilla  drop(Z)V  LVT: 0 this | 1 all | 2 inventory:Inventory | 3 removed:ItemStack
 * NeoForge drop(Z)V  LVT: 0 this | 1 all | 2 inventory:Inventory | 3 selected:ItemStack | 4 removed:ItemStack
 * </pre>
 *
 * Activating the shared mixin on Fabric would therefore fail outright — and "fixing" it to {@code name = "removed"}
 * would be <em>wrong</em>, not merely different: NeoForge's {@code selected} is {@code inventory.getSelectedItem()}
 * (the live stack that stays in the inventory), while {@code removed} is the split-off copy that is handed to the
 * dropped {@code ItemEntity}. Restocking the latter would top up an item that is already flying through the air.
 * <p>
 * The faithful Fabric expression is simply {@code inventory.getSelectedItem()} read at {@code TAIL}. Verified
 * equivalent: {@code Inventory#removeFromSelected(boolean)} → {@code removeItem} → {@code ContainerHelper.removeItem} →
 * {@code ItemStack#split}, which always returns a <em>copy</em> and leaves the original object in the inventory list
 * with a reduced count. So at {@code TAIL} {@code getSelectedItem()} returns exactly the object NeoForge's
 * {@code selected} refers to; when the slot is emptied both sides see {@code isEmpty()} and bail identically.
 * <p>
 * Injection point: vanilla {@code drop(Z)V} has a single exit (offset 55), so {@code TAIL} is unambiguous.
 * <p>
 * The shared {@code ServerPlayerMixin} is excluded from the Fabric source set (see {@code build.gradle.kts}); it stays
 * untouched as the NeoForge overlay so the NeoForge build remains byte-identical to upstream.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDropMixin {
    @Inject(method = "drop(Z)V", at = @At("TAIL"))
    private void ae2wtlib$restockAfterDrop(boolean all, CallbackInfo ci) {
        var self = (ServerPlayer) (Object) this;
        var inventory = self.getInventory();
        var item = inventory.getSelectedItem();
        if (item.isEmpty())
            return;

        AE2wtlibEvents.restock(self, item, item,
                stack -> inventory.setItem(inventory.getSelectedSlot(), stack));
    }
}
