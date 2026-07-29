package de.mari_023.ae2wtlib.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import de.mari_023.ae2wtlib.AE2wtlibEvents;

/**
 * Fabric replacement for NeoForge's {@code ArrowNockEvent} and (the bow half of) {@code ArrowLooseEvent} (W3, seam #9)
 * — restocking the arrow stack from the ME network when you draw and when you release.
 *
 * <h2>Why a mixin</h2>
 *
 * fabric-api has no projectile-weapon events; both NeoForge events are fired from patched vanilla item code.
 *
 * <h2>Binary verification (javap, both jars, 2026-07-29)</h2>
 *
 * <ul>
 * <li><strong>Nock:</strong> {@code EventHooks.onArrowNock(...)} is invoked at offset 33 of
 * {@code BowItem#use(Level,Player,InteractionHand)}, right after {@code foundProjectile} is computed and before the
 * {@code hasInfiniteMaterials} branch. {@code event.hasAmmo()} <em>is</em> {@code foundProjectile}, i.e.
 * {@code !player.getProjectile(bow).isEmpty()} — recomputed here rather than captured, so no {@code @Local} is
 * involved. {@code BowItem} is the ONLY class that fires {@code onArrowNock} in NeoForge 26.1.2.87.</li>
 * <li><strong>Loose:</strong> {@code EventHooks.onArrowLoose(...)} is invoked at offset 66 of
 * {@code BowItem#releaseUsing(ItemStack,Level,LivingEntity,int)Z}, after the empty-projectile early return —
 * {@code hasAmmo} is again {@code !projectile.isEmpty()}. (The crossbow half lives in {@link CrossbowItemMixin}.)</li>
 * <li>Neither method is overloaded in {@code BowItem}, so the bare method names select unambiguously.</li>
 * </ul>
 *
 * Both handlers pass the projectile stack as <em>both</em> {@code item} and {@code now} with a no-op setter, exactly
 * like upstream: {@code Player#getProjectile} returns the live inventory stack, which {@code restock} tops up in place.
 */
@Mixin(BowItem.class)
public class BowItemMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void ae2wtlib$restockOnArrowNock(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        ae2wtlib$restockProjectile(player, player.getItemInHand(hand));
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void ae2wtlib$restockOnArrowLoose(ItemStack bow, Level level, LivingEntity entity, int remainingTime,
            CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player)
            ae2wtlib$restockProjectile(player, bow);
    }

    @Unique
    private static void ae2wtlib$restockProjectile(Player player, ItemStack bow) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        ItemStack projectile = player.getProjectile(bow);
        // == `if (!event.hasAmmo()) return;`
        if (projectile.isEmpty())
            return;

        AE2wtlibEvents.restock(serverPlayer, projectile, projectile, _ -> {
        });
    }
}
