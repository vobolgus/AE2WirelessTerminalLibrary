package de.mari_023.ae2wtlib.recipeviewer;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.wut.WTDefinitions;

/**
 * ⚠ The {@code @me.shedaniel.rei.forge.REIPluginClient} annotation this class used to carry is <strong>NeoForge
 * only</strong>; it moved to the loader-overlay subclass {@code de.mari_023.ae2wtlib.neoforge.NeoForgeREIPlugin}. On
 * Fabric this class is registered directly as the {@code rei_client} entrypoint and carries no annotation at all — the
 * same split our AE2 fork made for {@code appeng.client.integration.rei.ReiClientPlugin}.
 *
 * <h2>⚠ The constructor must stay EMPTY (R8)</h2>
 *
 * REI instantiates its {@code rei_common}/{@code rei_client} entrypoints during <em>its own</em> loader entrypoint,
 * which on a client always runs before this mod has initialized. The AE2 fork hit exactly this on 2026-07-04: a
 * {@code static final} mod-loaded check crashed plugin construction. Anything touching this mod's or AE2's static state
 * must therefore stay inside a registration callback.
 * <p>
 * The rule is honoured here for free: there is no explicit constructor; {@link #getPluginProviderName()} returns a
 * compile-time {@code String} constant (javac inlines {@code AE2wtlibAPI.MOD_NAME}, so not even {@code AE2wtlibAPI}
 * gets loaded); and {@link WTDefinitions} — whose static initializer <em>throws</em> unless terminal registration has
 * already happened — is touched only from {@link #registerCategories}, which REI calls post-init on every reload.
 */
public class REIPlugin implements REIClientPlugin {
    @Override
    public String getPluginProviderName() {
        return AE2wtlibAPI.MOD_NAME;
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.addWorkstations(BuiltinPlugin.CRAFTING,
                EntryStacks.of(WTDefinitions.CRAFTING.universalTerminalStackWithEnergy()));
    }
}
