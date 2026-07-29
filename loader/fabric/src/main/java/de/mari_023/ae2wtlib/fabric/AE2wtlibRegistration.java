package de.mari_023.ae2wtlib.fabric;

import appeng.fabric.AE2FabricRegistration;

/**
 * Drives AE2WTLib's registration from AE2's {@code ae2:registration} addon entrypoint.
 *
 * <h2>Why not the mod initializer (found in W2)</h2>
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
 * AE2 invokes {@link #registerContent()} from the very end of {@code AppEngFabric.init(AppEngBase)}, the single static
 * method both dists funnel through, which pins all three at once: AE2 is fully registered, and the call rides whatever
 * dist-appropriate entrypoint AE2 itself chose, so both sides see the identical order "AE2 content, then AE2WTLib
 * content".
 * <p>
 * This replaces the fork-private {@code AppEngFabricMixin} that used to inject at the TAIL of the same method (the
 * entrypoint fires at exactly that position, so the switch is behaviour-preserving) - one less mixin target to
 * re-verify on every AE2 rebase.
 * <p>
 * The constructor must stay empty: Fabric Loader instantiates this class during AE2's own init, i.e. before
 * {@link #registerContent()} runs (AE2 dispatches the same entrypoint key earlier for part APIs).
 */
public class AE2wtlibRegistration implements AE2FabricRegistration {
    @Override
    public void registerContent() {
        AE2wtlibFabric.init();
    }
}
