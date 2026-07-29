package de.mari_023.ae2wtlib.fabric.mixin;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import de.mari_023.ae2wtlib.AE2wtlibEvents;

/**
 * Fabric replacement for NeoForge's {@code ItemEntityPickupEvent.Pre} (W3, seam #9) — the magnet card / restock "vacuum
 * straight into the ME network" behaviour.
 *
 * <h2>Why a mixin and not a fabric-api callback</h2>
 *
 * fabric-api 0.151.0 has <strong>no item-entity pickup event of any kind</strong> (verified: zero classes matching
 * {@code pickup}/{@code ItemEntity} in the whole API; {@code PlayerPickItemEvents} is creative middle-click pick-block,
 * something else entirely).
 *
 * <h2>Binary verification (javap, both jars, 2026-07-29)</h2>
 *
 * NeoForge patches {@code ItemEntity#playerTouch(Player)} to fire {@code EventHooks.fireItemPickupPre} at offset 26 —
 * after the {@code isClientSide} guard and after {@code itemStack}/{@code item}/{@code orgCount} are captured, but
 * <em>before</em> the pickup-delay and {@code target}-UUID checks and before {@code Inventory#add}. Upstream's
 * {@code EventPriority.LOWEST} handler then re-implements exactly those two default checks
 * ({@code event.canPickup().isDefault()} branch) before calling {@code insertStackInME}.
 * <p>
 * Injecting at {@code HEAD} reproduces that: no other mod can supply a {@code TriState} override on Fabric, so the
 * {@code isDefault} branch is the only reachable one, and the vanilla checks are replicated here verbatim. Mutating the
 * stack before vanilla's {@code Inventory#add} is intentional and is what upstream does — vanilla then picks up
 * whatever the ME network did not take.
 * <ul>
 * <li>{@code public void playerTouch(net.minecraft.world.entity.player.Player)} — same signature on both loaders.</li>
 * <li>{@code hasPickUpDelay()} is public; {@code target} is {@code private UUID} with a setter but no getter, so it is
 * {@code @Shadow}ed rather than access-widened (R9: AW field-widening has proved flaky in this workspace).</li>
 * <li>NeoForge takes its {@code orgCount} snapshot <em>after</em> the event (offset 45 {@code itemStack.copy()}), and
 * vanilla captures {@code orgCount} at offset 21 — i.e. after a HEAD injection. The stat/{@code take} accounting
 * therefore sees the post-insert count on both loaders.</li>
 * </ul>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    @Nullable
    private UUID target;

    @Shadow
    public abstract boolean hasPickUpDelay();

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void ae2wtlib$insertIntoMeNetwork(Player player, CallbackInfo ci) {
        var self = (ItemEntity) (Object) this;
        if (self.level().isClientSide())
            return;
        // == the `event.canPickup().isDefault()` branch of upstream's handler
        if (hasPickUpDelay())
            return;
        if (target != null && !target.equals(player.getUUID()))
            return;

        AE2wtlibEvents.insertStackInME(self, player);
    }
}
