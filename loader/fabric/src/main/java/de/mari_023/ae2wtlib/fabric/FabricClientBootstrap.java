package de.mari_023.ae2wtlib.fabric;

import org.jspecify.annotations.Nullable;

/**
 * Rendezvous between {@link AE2wtlibFabric#init()} and the Fabric <em>client</em> entrypoint.
 *
 * <h2>Why (W3 — the client-side half of the W2 §8.3 finding)</h2>
 *
 * Fabric Loader does not order entrypoints by mod dependency, and AE2WTLib's common init is deliberately not driven
 * from an entrypoint at all: it rides {@code AppEngFabricMixin} at the TAIL of {@code AppEngFabric#init}, which on a
 * <em>client</em> AE2 calls from its own {@code ClientModInitializer}. So {@code AE2wtlibFabricClient} can run either
 * before or after our own registration, depending on nothing more than mod-load order.
 * <p>
 * That is not theoretical: the first W3 {@code runClient} died with
 * {@code IllegalArgumentException: Cannot register handler as no payload type has been registered with name
 * "ae2wtlib:update_wut" for CLIENTBOUND PLAY} — our client entrypoint had run first, before
 * {@code FabricNetworkInit.registerPayloadTypes()}.
 * <p>
 * Rather than duplicate registrations or guess an order, the client entrypoint hands its AE2-/registration-dependent
 * work to this class and whichever of the two sides finishes last runs it. This class lives in the
 * <strong>main</strong> (server-safe) source set on purpose: it holds a plain {@link Runnable} and never names a client
 * type, so {@code AE2wtlibFabric} can call {@link #onCommonInitDone()} without leaking client classes onto the server
 * path. On a dedicated server the {@code Runnable} is simply never supplied and nothing runs.
 */
public final class FabricClientBootstrap {
    @Nullable
    private static Runnable clientInit;

    private static boolean commonInitDone;
    private static boolean executed;

    private FabricClientBootstrap() {}

    /**
     * Called by the client entrypoint with everything that must not run before {@link AE2wtlibFabric#init()}.
     */
    public static synchronized void deferUntilRegistered(Runnable clientInit) {
        FabricClientBootstrap.clientInit = clientInit;
        runIfReady();
    }

    /**
     * Called at the end of {@link AE2wtlibFabric#init()} on both dists.
     */
    public static synchronized void onCommonInitDone() {
        commonInitDone = true;
        runIfReady();
    }

    private static void runIfReady() {
        var init = clientInit;
        if (executed || !commonInitDone || init == null)
            return;
        executed = true;
        init.run();
    }
}
