package de.mari_023.ae2wtlib.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import de.mari_023.ae2wtlib.AE2wtlibEvents;

/**
 * Fabric replacement for NeoForge's {@code LivingEntityUseItemEvent.Finish} (W3, seam #9).
 *
 * <h2>Why a mixin and not a fabric-api callback</h2>
 *
 * fabric-api has no "finished using an item" event at all. The NeoForge event is also <em>result-rewriting</em>: the
 * handler may replace the stack that goes back into the hand, which only {@code @WrapOperation} can express.
 *
 * <h2>Binary verification (javap, both jars, 2026-07-29)</h2>
 *
 * NeoForge patches {@code LivingEntity#completeUsingItem} to
 * {@code EventHooks.onItemUseFinish(this, copy, remainingTicks, useItem.finishUsingItem(level, this))} where
 * {@code copy} is {@code useItem.copy()} taken <em>before</em> the call, and the return value replaces the result.
 * Wrapping the very same {@code ItemStack#finishUsingItem} invocation therefore reproduces the NeoForge shape exactly:
 * copy before, hook after, settable result.
 * <ul>
 * <li>vanilla {@code protected void completeUsingItem()} — the single {@code ItemStack.finishUsingItem} INVOKE sits at
 * offset 69; there is exactly one call site in the method.</li>
 * <li>{@code finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;}
 * — descriptor identical on both loaders.</li>
 * </ul>
 *
 * {@code completeUsingItem} also runs client-side, hence the {@code ServerPlayer} guard (which is what upstream's
 * handler does too).
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ae2wtlib$restockOnUseFinish(ItemStack useItem, Level level, LivingEntity entity,
            Operation<ItemStack> original) {
        // NeoForge: `ItemStack copy = useItem.copy();` immediately before the call - this is event.getItem().
        ItemStack before = useItem.copy();
        ItemStack result = original.call(useItem, level, entity);

        if (!(entity instanceof ServerPlayer player))
            return result;

        // event.getResultStack() / event::setResultStack
        var resultBox = new ItemStack[] { result };
        AE2wtlibEvents.restock(player, before, resultBox[0], stack -> resultBox[0] = stack);
        return resultBox[0];
    }
}
