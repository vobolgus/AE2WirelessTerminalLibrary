package de.mari_023.ae2wtlib.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import de.mari_023.ae2wtlib.AE2wtlibClientConfig;
import de.mari_023.ae2wtlib.fabric.AE2wtlibFabric;
import de.mari_023.ae2wtlib.fabric.config.FabricConfigStore;
import de.mari_023.ae2wtlib.fabric.network.FabricNet;
import de.mari_023.ae2wtlib.networking.RestockAmountPacket;
import de.mari_023.ae2wtlib.networking.UpdateRestockPacket;
import de.mari_023.ae2wtlib.networking.UpdateWUTPackage;

/**
 * Fabric client entrypoint - the twin of {@code de.mari_023.ae2wtlib.AE2wtlibClient} plus the client-side
 * {@code @SubscribeEvent} handlers that upstream keeps in {@code AE2wtlibForge}.
 * <p>
 * Contains only order-independent client wiring: Fabric Loader gives no ordering guarantee between this and AE2's
 * client entrypoint, so anything that needs AE2 (or this mod's own menus) must go through the {@code AppEngFabricMixin}
 * path instead - see {@link AE2wtlibFabric}'s javadoc.
 * <p>
 * NOTE (playbook Part 8): never reuse one stateful class across multiple fabric.mod.json entrypoint keys - loader
 * constructs a fresh instance per key. This class is deliberately separate from {@link AE2wtlibFabric}.
 */
public class AE2wtlibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // NOTE: the common init() is NOT called here - AppEngFabricMixin drives it at the right moment on both
        // dists (see AE2wtlibFabric's javadoc). Everything below is order-independent client-only wiring.
        FabricConfigStore.create(AE2wtlibClientConfig.FILE_NAME, AE2wtlibClientConfig::register);

        // ClientPlayNetworking is client-only, so the serverbound sender is injected rather than referenced.
        FabricNet.setClientPacketSender(ClientPlayNetworking::send);

        ClientPlayNetworking.registerGlobalReceiver(UpdateWUTPackage.ID,
                (payload, context) -> context.client().execute(() -> payload.processPacketData(context.player())));
        ClientPlayNetworking.registerGlobalReceiver(UpdateRestockPacket.ID,
                (payload, context) -> context.client().execute(() -> payload.processPacketData(context.player())));
        ClientPlayNetworking.registerGlobalReceiver(RestockAmountPacket.ID,
                (payload, context) -> context.client().execute(() -> payload.processPacketData(context.player())));

        // W2-STUB (W5): AE2wtlib.registerScreens -> InitScreens.register(InitScreens.MenuScreenRegistrar, ...)
        // (our AE2 fork's loader-neutral seam; see NeoForgeScreens for the NeoForge half).
        // W2-STUB (W3): ClientTickEvent.Post -> ClientTickEvents.END_CLIENT_TICK -> AE2wtlibClient.clientTick()
        // W2-STUB (W3): InputEvent.MouseScrollingEvent -> cancellable mixin on MouseHandler#onScroll
        // (fabric-api has no cancellable scroll event) -> AE2wtlibClient.mouseScroll(...)
        // DEFERRED (W7 polish): IConfigScreenFactory has no in-tree Fabric equivalent; ModMenu is the usual host and
        // a config screen is not a parity requirement.
    }
}
