package de.mari_023.ae2wtlib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.neoforge.NeoForgeConfigStore;

/**
 * NeoForge-only client mod class. The two behaviour bodies moved to the loader-neutral {@link AE2wtlibClientEvents} in
 * W3 of the Fabric port so the Fabric client entrypoint and {@code MouseHandlerMixin} can reach them; the methods here
 * stay as the NeoForge event adapters.
 */
@Mod(value = AE2wtlibAPI.MOD_NAME, dist = Dist.CLIENT)
public class AE2wtlibClient {
    public AE2wtlibClient(IEventBus modEventBus, ModContainer modContainer) {
        NeoForgeConfigStore.register(modContainer, ModConfig.Type.CLIENT, AE2wtlibClientConfig.FILE_NAME,
                AE2wtlibClientConfig::register);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    public static void clientTick() {
        AE2wtlibClientEvents.clientTick();
    }

    public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
        if (AE2wtlibClientEvents.mouseScroll(event.getScrollDeltaY()))
            event.setCanceled(true);
    }
}
