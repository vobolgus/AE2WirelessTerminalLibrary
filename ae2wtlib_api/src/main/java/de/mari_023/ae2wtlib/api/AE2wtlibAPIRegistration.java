package de.mari_023.ae2wtlib.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import de.mari_023.ae2wtlib.api.registration.AddTerminalEvent;
import de.mari_023.ae2wtlib.api.registration.UpgradeHelper;

/**
 * The API module's registration step, extracted verbatim out of {@code AE2wtlibAPIEntrypoint} so both loaders can drive
 * it. On NeoForge it is still called from the {@code RegisterEvent(Registries.ITEM)} listener; on Fabric the loader
 * entrypoint calls it directly.
 * <p>
 * <strong>Ordering contract</strong> (identical on both loaders):
 * <ol>
 * <li>the mod's items must already be registered and bound — {@link AddTerminalEvent} builds {@code WTDefinition}s that
 * resolve the universal-terminal item,</li>
 * <li>every {@code AddTerminalEvent.register(...)} handler must already be installed,</li>
 * <li>the data component types are flushed <em>last</em>, because {@code WTDefinitionBuilder#addTerminal} adds one
 * {@code has_<name>_terminal} component per terminal while the event runs.</li>
 * </ol>
 */
@ApiStatus.Internal
public final class AE2wtlibAPIRegistration {
    private AE2wtlibAPIRegistration() {}

    public static void register() {
        AddTerminalEvent.run();
        UpgradeHelper.addUpgrades();
        for (var entry : AE2wtlibComponents.DR.entrySet())
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, entry.getKey(), entry.getValue());
    }
}
