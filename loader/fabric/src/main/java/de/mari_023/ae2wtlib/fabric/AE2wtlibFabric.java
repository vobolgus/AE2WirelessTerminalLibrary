package de.mari_023.ae2wtlib.fabric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric common entrypoint - the twin of {@code de.mari_023.ae2wtlib.AE2wtlibForge}.
 * <p>
 * W1 (skeleton) only proves the module builds; every call below is still a stub. The list is kept in the exact order of
 * {@code AE2wtlibForge}'s constructor + its {@code @SubscribeEvent} handlers so the W2/W3 waves can tick it off top to
 * bottom (playbook Part 8: "audit every {@code set*(} seam against {@code init()} - an unwired client packet sender
 * crashed only in prod").
 */
public class AE2wtlibFabric implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("ae2wtlib");

    @Override
    public void onInitialize() {
        // W1-STUB: new AE2wtlibAPIImplementation();
        // W1-STUB: modContainer.registerConfig(ModConfig.Type.COMMON, AE2wtlibConfig.SPEC, "ae2wtlib.toml")
        // -> night-config store behind an AE2wtlibConfigStore seam (crib:
        // appeng.core.config.ConfigStore + appeng.fabric.config.FabricConfigStore).
        // W1-STUB: AE2wtlibItems.DR.register(modEventBus)
        // -> plain Registry.register; golden rule #7: Properties.setId BEFORE construction,
        // then item.registerBlocks(Item.BY_BLOCK, item) / bindComponents.
        // W1-STUB: RegisterEvent(Registries.MENU) -> AE2wtlib.registerMenus()
        // W1-STUB: RegisterEvent(Registries.ITEM) -> AE2wtlib.registerTerminals(), registerRecipes(),
        // registerHotkeyActions(), AE2wtlibCreativeTab.init()
        // W1-STUB: FMLCommonSetupEvent -> AE2wtlib.registerGridLinkables(), registerUpgrades()
        // W1-STUB: BuildCreativeModeTabContentsEvent -> AE2wtlib.addToCreativeTab()
        // (fabric-api: ItemGroupEvents.modifyEntriesEvent)
        // W1-STUB: RegisterPayloadHandlersEvent -> PayloadTypeRegistry.playC2S/playS2C +
        // ServerPlayNetworking.registerGlobalReceiver for the 6 AE2wtlib packets.
        // W1-STUB: RegisterCapabilitiesEvent (Capabilities.Energy.ITEM for the 3 powered terminals)
        // -> EnergyStorage.ITEM.registerForItems(...) with
        // appeng.fabric.transfer.PoweredItemEnergyStorage (crib: AE2's InitApiLookup:255).
        // W1-STUB: AE2wtlibAdditionalComponents.init()
        // W1-STUB: AE2wtlib.ATTACHMENT_TYPES.register(modEventBus)
        // -> fabric-api AttachmentRegistry (or a weak map, cf. AE2's FabricPlayerCtrlAttachment).
        //
        // Event handlers still to be re-homed (NeoForge event -> Fabric mechanism):
        // W1-STUB: LivingEntityUseItemEvent.Finish -> mixin on LivingEntity#completeUsingItem
        // W1-STUB: PlayerInteractEvent.RightClickBlock -> UseBlockCallback / mixin (LOWEST priority!)
        // W1-STUB: PlayerInteractEvent.EntityInteractSpecific -> UseEntityCallback / mixin
        // W1-STUB: ItemEntityPickupEvent.Pre -> mixin on ItemEntity#playerTouch
        // W1-STUB: ArrowNockEvent / ArrowLooseEvent -> mixins on BowItem/CrossbowItem
        // W1-STUB: ClientTickEvent.Post -> client entrypoint (see AE2wtlibFabricClient)
        // W1-STUB: InputEvent.MouseScrollingEvent -> client entrypoint (see AE2wtlibFabricClient)
        LOG.info("AE2wtlib Fabric platform layer loaded (W1 skeleton - no features wired yet).");
    }
}
