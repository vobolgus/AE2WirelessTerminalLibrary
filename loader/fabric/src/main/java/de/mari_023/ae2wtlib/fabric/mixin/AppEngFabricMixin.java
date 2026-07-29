package de.mari_023.ae2wtlib.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.fabric.AppEngFabric;

import de.mari_023.ae2wtlib.fabric.AE2wtlibFabric;

/**
 * Drives AE2WTLib's registration from the very end of AE2's own initialization.
 *
 * <h2>Why a mixin and not the mod initializer (found in W2)</h2>
 *
 * Fabric Loader does <strong>not</strong> order entrypoint invocation by mod dependencies - verified in this
 * workspace's dev run, where {@code de.mari_023.ae2wtlib.fabric.AE2wtlibFabric} was registered (and therefore invoked)
 * <em>before</em> {@code appeng.fabric.AppEngFabric}, despite {@code "depends": {"ae2": "*"}}. AE2WTLib genuinely needs
 * AE2 to be up first:
 * <ul>
 * <li>{@code ItemWT}'s constructor reads {@code AEConfig.instance()} - null until AE2's {@code FabricConfigStore} ran
 * (this is what crashed the first W2 {@code runServer});</li>
 * <li>{@code AEItems.WIRELESS_CRAFTING_TERMINAL} must be bound (and {@code AEItemsMixin}'s swap must have happened)
 * before this mod's terminals are built;</li>
 * <li>item raw registry ids must land in the same relative order on client and server (playbook Part 10). AE2 itself
 * registers its content from {@code AppEngFabric.init}, which it calls from the {@code main} entrypoint on a dedicated
 * server but from the <em>{@code client}</em> entrypoint on a client - so even a correct entrypoint order would place
 * this mod's items before AE2's on a client and after them on a server.</li>
 * </ul>
 * Injecting at the TAIL of {@code AppEngFabric.init(AppEngBase)} pins all three: AE2 is fully registered, and the call
 * happens on whatever dist-appropriate entrypoint AE2 itself chose, so both sides see the identical order "AE2 content,
 * then AE2WTLib content".
 * <p>
 * AE2 currently exposes no addon entrypoint for this ({@code ae2:client_registration} is client-only and internal). The
 * durable fix is an {@code ae2:registration} entrypoint in the AE2 fork; until then this single injection carries it.
 * <strong>Re-verify this target on every AE2 rebase</strong> - {@code AppEngFabric} only exists in our fork.
 */
@Mixin(AppEngFabric.class)
public class AppEngFabricMixin {
    @Inject(method = "init(Lappeng/core/AppEngBase;)V", at = @At("TAIL"))
    private static void ae2wtlib$init(CallbackInfo ci) {
        AE2wtlibFabric.init();
    }
}
