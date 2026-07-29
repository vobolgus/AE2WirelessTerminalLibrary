package de.mari_023.ae2wtlib.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MouseHandler;

import de.mari_023.ae2wtlib.AE2wtlibClientEvents;

/**
 * Fabric replacement for NeoForge's {@code InputEvent.MouseScrollingEvent} (W3, seam #9) — shift+scroll cycles the
 * active terminal of a held Wireless Universal Terminal.
 *
 * <h2>Why a mixin and not a fabric-api callback</h2>
 *
 * fabric-api 0.151.0 has <strong>no raw mouse-scroll event</strong>. The only two scroll APIs are
 * {@code ClientHotbarScrollEvents} (fired from a {@code @WrapOperation} on {@code Inventory#setSelectedSlot}, so it
 * only sees hotbar slot <em>changes</em> — shift+scroll on a terminal is not one) and {@code ScreenMouseEvents}
 * (per-{@code Screen}, and this behaviour deliberately only applies when no screen is open). A cancellable mixin is the
 * only faithful option.
 *
 * <h2>Binary verification (javap, both jars, 2026-07-29)</h2>
 *
 * NeoForge fires {@code ClientHooks.onMouseScroll(this, horizontal, vertical)} at offset 287 of
 * {@code MouseHandler#onScroll(JDD)V} and {@code return}s if it is cancelled. The instruction immediately after it is
 * {@code minecraft.player.isSpectator()}. In the vanilla method {@code LocalPlayer#isSpectator()} is invoked
 * <strong>exactly once</strong> (offset 257), so {@code @At(INVOKE, target = LocalPlayer.isSpectator)} pins the
 * identical point:
 * <ul>
 * <li>after the {@code screen != null} branch (so {@code AE2wtlibClientEvents#mouseScroll}'s own screen check is
 * belt-and-braces),</li>
 * <li>after the {@code player != null} check,</li>
 * <li>after {@code ScrollWheelHandler#onMouseScroll} consumed the accumulator and the both-axes-zero early return — so
 * sub-threshold scrolls never reach either loader's hook.</li>
 * </ul>
 * {@code ci.cancel()} returns from the void method, which is exactly NeoForge's cancelled path.
 * <p>
 * The delta passed on is the raw {@code yOffset} parameter rather than the sensitivity-scaled local NeoForge uses; only
 * its sign and zero-ness are consumed and the scaling factor is strictly positive, so this is equivalent.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onScroll(JDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"), cancellable = true)
    private void ae2wtlib$cycleUniversalTerminal(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (AE2wtlibClientEvents.mouseScroll(yOffset))
            ci.cancel();
    }
}
