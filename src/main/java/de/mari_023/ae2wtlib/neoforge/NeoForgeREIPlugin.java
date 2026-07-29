package de.mari_023.ae2wtlib.neoforge;

import me.shedaniel.rei.forge.REIPluginClient;

import de.mari_023.ae2wtlib.recipeviewer.REIPlugin;

/**
 * NeoForge overlay: carries the loader-specific {@code @REIPluginClient} annotation that REI's NeoForge build scans
 * for. Fabric has no such annotation — there the shared {@link REIPlugin} is wired through the {@code rei_client}
 * entrypoint in {@code fabric.mod.json} instead.
 * <p>
 * Same split our AE2 fork uses for {@code appeng.client.integration.rei.NeoForgeReiClientPlugin}. All behaviour stays
 * in the shared class so an upstream rebase only ever touches one copy.
 */
@REIPluginClient
public class NeoForgeREIPlugin extends REIPlugin {
}
