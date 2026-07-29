package de.mari_023.ae2wtlib.fabric.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint - the twin of {@code de.mari_023.ae2wtlib.AE2wtlibClient} (the
 * {@code @Mod(dist = Dist.CLIENT)} class) plus the client-side {@code @SubscribeEvent} handlers that upstream keeps in
 * {@code AE2wtlibForge}.
 * <p>
 * NOTE (playbook Part 8): never reuse one stateful class across multiple fabric.mod.json entrypoint keys - loader
 * constructs a fresh instance per key, which trips singleton guards. This class is deliberately separate from
 * {@link de.mari_023.ae2wtlib.fabric.AE2wtlibFabric}.
 */
public class AE2wtlibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // W1-STUB: modContainer.registerConfig(ModConfig.Type.CLIENT, AE2wtlibClientConfig.SPEC,
        // "ae2wtlib-client.toml") -> the same config-store seam as the common config.
        // W1-STUB: registerExtensionPoint(IConfigScreenFactory, ConfigurationScreen::new)
        // -> no Fabric equivalent in-tree; ModMenu is the usual host. DEFERRED (W7 polish):
        // a config screen is not required for feature parity.
        // W1-STUB: AE2wtlib.registerScreens(RegisterMenuScreensEvent)
        // -> MenuScreens.register via AE2's loader-neutral InitScreens.MenuScreenRegistrar
        // (present in OUR AE2 fork only - see PORTING_NOTES "AE2 API divergences").
        // W1-STUB: ClientTickEvent.Post -> ClientTickEvents.END_CLIENT_TICK -> AE2wtlibClient.clientTick()
        // W1-STUB: InputEvent.MouseScrollingEvent -> mixin on MouseHandler#onScroll (fabric-api has no
        // cancellable scroll event) -> AE2wtlibClient.mouseScroll(...)
        // W1-STUB: client packet receivers (UpdateWUTPackage, UpdateRestockPacket, RestockAmountPacket)
        // -> ClientPlayNetworking.registerGlobalReceiver
    }
}
