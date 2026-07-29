package de.mari_023.ae2wtlib.api;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge entrypoint of the API module.
 * <p>
 * <strong>NeoForge overlay — excluded from the Fabric source set</strong> (see {@code loader/fabric/build.gradle.kts}).
 * The Fabric twin is {@code de.mari_023.ae2wtlib.fabric.AE2wtlibFabric}, which calls
 * {@link AE2wtlibAPIRegistration#register()} directly; the registration body itself is shared.
 */
@Mod(AE2wtlibAPI.API_MOD_NAME)
public class AE2wtlibAPIEntrypoint {
    public AE2wtlibAPIEntrypoint(IEventBus modEventBus) {
        modEventBus.addListener((RegisterEvent event) -> {
            if (!event.getRegistryKey().equals(Registries.ITEM)) {
                return;
            }
            AE2wtlibAPIRegistration.register();
        });
    }
}
