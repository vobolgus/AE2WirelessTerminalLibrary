package de.mari_023.ae2wtlib.fabric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import appeng.core.definitions.AEItems;

import de.mari_023.ae2wtlib.AE2wtlib;
import de.mari_023.ae2wtlib.AE2wtlibAPIImplementation;
import de.mari_023.ae2wtlib.AE2wtlibAdditionalComponents;
import de.mari_023.ae2wtlib.AE2wtlibConfig;
import de.mari_023.ae2wtlib.AE2wtlibCreativeTab;
import de.mari_023.ae2wtlib.AE2wtlibItems;
import de.mari_023.ae2wtlib.Ae2wtlibItemHooks;
import de.mari_023.ae2wtlib.api.AE2wtlibAPIRegistration;
import de.mari_023.ae2wtlib.api.Ae2wtlibNet;
import de.mari_023.ae2wtlib.api.Ae2wtlibPlatform;
import de.mari_023.ae2wtlib.attachment.Ae2wtlibAttachments;
import de.mari_023.ae2wtlib.fabric.config.FabricConfigStore;
import de.mari_023.ae2wtlib.fabric.event.Ae2wtlibFabricEvents;
import de.mari_023.ae2wtlib.fabric.network.FabricNet;
import de.mari_023.ae2wtlib.fabric.network.FabricNetworkInit;
import de.mari_023.ae2wtlib.registration.Ae2wtlibItemFactory;
import de.mari_023.ae2wtlib.wct.ItemWCT;
import de.mari_023.ae2wtlib.wct.WrappedPlayerInventory;

/**
 * Fabric common entrypoint - the twin of {@code de.mari_023.ae2wtlib.AE2wtlibForge}.
 *
 * <h2>⚠ Where the real initialization happens</h2>
 *
 * <strong>Not here.</strong> {@link #init()} is driven from the TAIL of AE2's own {@code AppEngFabric.init}, via
 * {@code de.mari_023.ae2wtlib.fabric.mixin.AppEngFabricMixin} - Fabric Loader does not order entrypoints by mod
 * dependency, and AE2 registers its content from a different entrypoint per dist. See that mixin's javadoc for the full
 * reasoning and the three concrete failures it prevents (it is the W2 gate finding).
 * <p>
 * This class stays registered as the {@code main} entrypoint so the mod has one, and so that a future AE2 addon
 * entrypoint can be switched to without touching anything else.
 */
public class AE2wtlibFabric implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("ae2wtlib");

    private static boolean initialized;

    @Override
    public void onInitialize() {
        // Intentionally empty - see the class javadoc. AppEngFabricMixin calls init().
    }

    /**
     * The loader-neutral half of {@code AE2wtlibForge}'s constructor, in the same order. Called exactly once, from
     * {@code AppEngFabricMixin}; the guard makes a future switch to a real AE2 addon entrypoint a one-line change.
     */
    public static synchronized void init() {
        if (initialized)
            return;
        initialized = true;

        // 1. Inject the loader-specific seam implementations. This has to happen before anything class-loads
        // AE2wtlibItems (its static initializer builds every ItemDefinition through the factory).
        Ae2wtlibPlatform.init(modId -> FabricLoader.getInstance().isModLoaded(modId));
        Ae2wtlibNet.init(new FabricNet());
        Ae2wtlibAttachments.init(new FabricAttachments());
        Ae2wtlibItemFactory.init(new FabricItemFactory());
        Ae2wtlibItemHooks.init(new FabricItemHooks());
        WrappedPlayerInventory.factory = FabricWrappedPlayerInventory::new;

        new AE2wtlibAPIImplementation();

        // 2. Config (NeoForge: modContainer.registerConfig(COMMON, ...)).
        FabricConfigStore.create(AE2wtlibConfig.FILE_NAME, AE2wtlibConfig::register);

        // 3. Content. NeoForge drives this from RegisterEvent(ITEM)/RegisterEvent(MENU); on Fabric the registries are
        // already open, so the calls happen inline - in the order the events would have fired.
        AE2wtlibItems.init();
        AE2wtlibAdditionalComponents.init();
        AE2wtlib.registerMenus();
        AE2wtlib.registerTerminals();
        AE2wtlibAPIRegistration.register();
        AE2wtlib.registerRecipes();
        AE2wtlib.registerHotkeyActions();
        AE2wtlibCreativeTab.init();

        // 4. NeoForge: FMLCommonSetupEvent.
        AE2wtlib.registerGridLinkables();
        AE2wtlib.registerUpgrades();

        // NeoForge: BuildCreativeModeTabContentsEvent.
        // ⚠ W3 correction to W2 (§8.5): this must NOT be called here. MC 26.1 binds item data components lazily -
        // DataComponentInitializers bakes them together with the reloadable server resources, long after mod init -
        // so building the tab's ItemStacks during registration throws "Components not bound yet" and the dedicated
        // server never boots. AE2wtlibCreativeTab#buildDisplayItems now fills the list itself on first use, which is
        // the moment the NeoForge event fires as well. Nothing to do here.

        // 5. Networking. Payload TYPES must be registered on both sides; the client receivers live in the client
        // entrypoint because ClientPlayNetworking is client-only.
        FabricNetworkInit.registerPayloadTypes();
        FabricNetworkInit.registerServerReceivers();

        verifyWirelessCraftingTerminalSwap();

        // 6. The restock event surface (NeoForge: the @SubscribeEvent handlers on AE2wtlibForge). Only the two
        // interaction events map onto fabric-api callbacks faithfully; the other four are mixins declared in
        // ae2wtlib.fabric.platform.mixins.json. See Ae2wtlibFabricEvents for the per-event rationale (R3).
        Ae2wtlibFabricEvents.register();

        // W2-STUB (W4): RegisterCapabilitiesEvent -> EnergyStorage.ITEM.registerForItems(...) with AE2's
        // appeng.fabric.transfer.PoweredItemEnergyStorage for the 3 powered terminals.
        LOG.info("AE2wtlib Fabric platform layer initialized (W3: event surface).");
    }

    /**
     * Risk R6: this mod does not register the wireless crafting terminal itself - {@code AEItemsMixin} intercepts AE2's
     * own item factory and swaps in {@link ItemWCT}. Our AE2 fork changed <em>how</em> items are registered
     * ({@code AEItemEntry} instead of {@code DeferredItem}), so the swap has to be verified rather than assumed. This
     * runs as early as the item is resolvable and fails loudly rather than leaving a subtly wrong terminal in the game.
     */
    private static void verifyWirelessCraftingTerminalSwap() {
        var item = AEItems.WIRELESS_CRAFTING_TERMINAL.asItem();
        if (!(item instanceof ItemWCT))
            throw new IllegalStateException("AEItemsMixin did not take: ae2:wireless_crafting_terminal is a "
                    + item.getClass().getName() + ", expected " + ItemWCT.class.getName()
                    + ". Wireless crafting terminals will not work.");
        LOG.debug("AEItemsMixin verified: ae2:wireless_crafting_terminal is an ItemWCT");
    }
}
