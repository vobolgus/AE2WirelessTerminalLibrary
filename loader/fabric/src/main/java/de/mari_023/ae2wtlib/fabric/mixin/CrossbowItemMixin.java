package de.mari_023.ae2wtlib.fabric.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import de.mari_023.ae2wtlib.AE2wtlibEvents;

/**
 * Fabric replacement for the crossbow half of NeoForge's {@code ArrowLooseEvent} (W3, seam #9).
 *
 * <h2>Binary verification (javap, both jars, 2026-07-29)</h2>
 *
 * NeoForge fires {@code EventHooks.onArrowLoose(crossbow, level, player, 1, true)} at offset 36 of
 * {@code CrossbowItem#performShooting(Level,LivingEntity,InteractionHand,ItemStack,float,float,LivingEntity)} — i.e.
 * inside the {@code level instanceof ServerLevel} branch, inside the {@code entity instanceof Player} branch, before
 * the {@code CHARGED_PROJECTILES} component is cleared. Note {@code hasAmmo} is hard-coded {@code true} there, so
 * upstream's {@code !event.hasAmmo()} guard can never trigger for crossbows; the empty-stack case is handled by
 * {@code restock} itself ({@code if (item.isEmpty()) return;}).
 * <p>
 * {@code releaseUsing} is <em>not</em> the right target: for crossbows it only reports whether the shot is ready.
 * {@code performShooting} is not overloaded, so the bare name selects unambiguously.
 */
@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
    @Inject(method = "performShooting", at = @At("HEAD"))
    private void ae2wtlib$restockOnArrowLoose(Level level, LivingEntity entity, InteractionHand hand,
            ItemStack crossbow, float velocity, float inaccuracy, @Nullable LivingEntity target, CallbackInfo ci) {
        if (!(level instanceof ServerLevel))
            return;
        if (!(entity instanceof ServerPlayer player))
            return;

        ItemStack projectile = player.getProjectile(crossbow);
        AE2wtlibEvents.restock(player, projectile, projectile, _ -> {
        });
    }
}
