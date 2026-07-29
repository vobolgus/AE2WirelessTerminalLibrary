package de.mari_023.ae2wtlib.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import de.mari_023.ae2wtlib.AE2wtlibClientConfig;
import de.mari_023.ae2wtlib.AE2wtlibClientEvents;
import de.mari_023.ae2wtlib.fabric.AE2wtlibFabric;
import de.mari_023.ae2wtlib.fabric.FabricClientBootstrap;
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

        // NeoForge: ClientTickEvent.Post. Exact fabric-api counterpart; no ordering or cancellation involved.
        ClientTickEvents.END_CLIENT_TICK.register(_ -> AE2wtlibClientEvents.clientTick());

        // ⚠ Everything below depends on AE2wtlibFabric.init() having run (payload TYPES, MenuTypes, AE2 itself), and
        // Fabric Loader gives NO ordering guarantee between this entrypoint and AE2's - which is what drives our
        // common init. The first W3 runClient proved it: registering the S2C receivers here died with
        // "no payload type has been registered with name ae2wtlib:update_wut". FabricClientBootstrap runs the block
        // as soon as BOTH sides are ready, whichever finishes last.
        FabricClientBootstrap.deferUntilRegistered(() -> {
            ClientPlayNetworking.registerGlobalReceiver(UpdateWUTPackage.ID,
                    (payload, context) -> context.client()
                            .execute(() -> payload.processPacketData(context.player())));
            ClientPlayNetworking.registerGlobalReceiver(UpdateRestockPacket.ID,
                    (payload, context) -> context.client()
                            .execute(() -> payload.processPacketData(context.player())));
            ClientPlayNetworking.registerGlobalReceiver(RestockAmountPacket.ID,
                    (payload, context) -> context.client()
                            .execute(() -> payload.processPacketData(context.player())));

            // NeoForge: AE2wtlib.registerScreens via RegisterMenuScreensEvent (NeoForgeScreens).
            FabricScreens.registerScreens();
        });

        // NeoForge: InputEvent.MouseScrollingEvent. fabric-api has NO raw scroll event, so this one is a cancellable
        // mixin instead - de.mari_023.ae2wtlib.fabric.mixin.client.MouseHandlerMixin (see its javadoc for the
        // javap-verified injection point). Nothing to register here.
        //
        // Hotkeys/keybinds: nothing to do here either. AE2wtlib.registerHotkeyActions() (driven from the common
        // init) goes through AE2's HotkeyActions -> Hotkeys.registerHotkey, and AE2's own client entrypoint then
        // registers every accumulated KeyMapping centrally via KeyMappingHelper. Our registration rides
        // AppEngFabricMixin at the TAIL of AppEngFabric#init, which on a client runs from AE2's
        // onInitializeClient BEFORE its Hotkeys.finalizeRegistration - so the three ae2wtlib_* hotkeys are included.
        //
        // Item models/properties: none. All five items use plain JSON item models (no ItemModel codec, no
        // RangeSelectItemModelProperty, no tint source), so there is no client-side model registration to mirror.
        //
        // DEFERRED (W7 polish): IConfigScreenFactory has no in-tree Fabric equivalent; ModMenu is the usual host and
        // a config screen is not a parity requirement.
    }
}
