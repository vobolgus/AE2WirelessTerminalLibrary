package de.mari_023.ae2wtlib.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import de.mari_023.ae2wtlib.AE2wtlibEvents;
import de.mari_023.ae2wtlib.api.AE2wtlibAPI;

/**
 * The two members of the restock event surface (W3, seam #9) that fabric-api <em>can</em> express faithfully:
 * NeoForge's {@code PlayerInteractEvent.RightClickBlock} and {@code PlayerInteractEvent.EntityInteractSpecific}.
 *
 * <h2>Why callbacks here, mixins elsewhere (R3)</h2>
 *
 * Upstream registers both at {@code EventPriority.LOWEST} and skips when {@code event.isCanceled()}. Those two
 * properties have exact fabric-api counterparts, which a TAIL/HEAD mixin could <em>not</em> reproduce (a mixin sees
 * neither other mods' handlers nor their cancellations):
 * <ul>
 * <li><strong>{@code EventPriority.LOWEST}</strong> → an event {@link #LAST_PHASE phase} ordered after
 * {@link Event#DEFAULT_PHASE}. Phases are a property of the singleton {@code Event} object, so the ordering applies
 * across all mods — every listener that did not opt into a later phase runs first.</li>
 * <li><strong>{@code event.isCanceled()}</strong> → fabric-api's array-backed invokers stop at the first listener
 * returning something other than {@code InteractionResult.PASS}. A cancelled interaction therefore never reaches a
 * late-phase listener at all, which is precisely upstream's early return. Our own listeners always return {@code PASS}
 * so they never cancel anything themselves.</li>
 * </ul>
 *
 * <h2>Injection points (verified against fabric-api 0.151.0+26.1.2)</h2>
 * <ul>
 * <li>{@code UseBlockCallback} is fired from {@code @Inject(HEAD)} on {@code ServerPlayerGameMode#useItemOn} — the very
 * method NeoForge fires {@code RightClickBlock} from (offset 41). Same instruction region, same semantics.</li>
 * <li>{@code UseEntityCallback} is fired from {@code ServerGamePacketListenerImpl#handleInteract}, which is where
 * NeoForge fires {@code CommonHooks.onInteractEntityAt} = {@code EntityInteractSpecific}. ⚠ MC 26.1 merged the two
 * NeoForge entity-interact events at the vanilla level: {@code Entity#interactAt} no longer exists and
 * {@code ServerboundInteractPacket} is a flat record, so there is exactly one entity-interaction path and one callback
 * for it.</li>
 * </ul>
 *
 * Both callbacks also fire client-side (fabric-api mirrors them onto {@code MultiPlayerGameMode} /
 * {@code Minecraft#startUseItem}); the {@code ServerPlayer} guard keeps every mutation server-authoritative, matching
 * upstream and the pack's live multiplayer server.
 */
public final class Ae2wtlibFabricEvents {
    /**
     * NeoForge's {@code EventPriority.LOWEST}, expressed as a phase ordered after everything else.
     */
    private static final Identifier LAST_PHASE = AE2wtlibAPI.id("restock_last");

    private Ae2wtlibFabricEvents() {}

    public static void register() {
        UseBlockCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LAST_PHASE);
        UseBlockCallback.EVENT.register(LAST_PHASE, (player, level, hand, hitResult) -> {
            restockHeldItem(player, hand);
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LAST_PHASE);
        UseEntityCallback.EVENT.register(LAST_PHASE, (player, level, hand, entity, hitResult) -> {
            restockHeldItem(player, hand);
            return InteractionResult.PASS;
        });
    }

    /**
     * Upstream passes {@code event.getItemStack()} as both {@code item} and {@code now} and writes the result back with
     * {@code player.setItemInHand(hand, stack)} - mirrored exactly.
     */
    private static void restockHeldItem(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        var item = serverPlayer.getItemInHand(hand);
        AE2wtlibEvents.restock(serverPlayer, item, item, stack -> serverPlayer.setItemInHand(hand, stack));
    }
}
