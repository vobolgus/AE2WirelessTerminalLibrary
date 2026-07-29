package de.mari_023.ae2wtlib.fabric.gametest;

import java.util.ArrayList;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.CreativeModeTab;

import appeng.api.features.HotkeyAction;
import appeng.core.definitions.AEItems;
import appeng.fabric.AE2FabricRegistration;
import appeng.hotkeys.HotkeyActions;

import de.mari_023.ae2wtlib.AE2wtlibItems;
import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.fabric.AE2wtlibRegistration;
import de.mari_023.ae2wtlib.networking.CycleTerminalPacket;
import de.mari_023.ae2wtlib.networking.SelectTerminalPacket;
import de.mari_023.ae2wtlib.networking.TerminalSettingsPacket;
import de.mari_023.ae2wtlib.wat.WATMenu;
import de.mari_023.ae2wtlib.wct.ItemWCT;
import de.mari_023.ae2wtlib.wct.TrashMenu;
import de.mari_023.ae2wtlib.wct.WCTMenu;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMenu;
import de.mari_023.ae2wtlib.wet.WETMenu;

/**
 * Registration-surface guards: everything {@code AE2wtlibFabric.init()} is supposed to have done by the time a
 * dedicated server is up.
 * <p>
 * ⚠ Every test class must be listed in the {@code fabric-gametest} entrypoint array of
 * {@code src/gametest/resources/fabric.mod.json}. fabric-gametest-api-v1 discovers tests <em>only</em> by reflecting
 * over the classes named there — an unlisted class is silently skipped, with at most a "No methods with the GameTest
 * annotation were found in {}" debug line.
 * <p>
 * Test methods must be {@code public}, non-{@code static}, {@code void} and take exactly one {@link GameTestHelper}.
 * The default {@code structure} is {@code fabric-gametest-api-v1:empty}, so none of these need a structure NBT.
 */
public class RegistrationTests {
    /** R6 / {@code AEItemsMixin}: AE2's own wireless crafting terminal must be our {@link ItemWCT}. */
    @GameTest
    public void wirelessCraftingTerminalIsSwapped(GameTestHelper helper) {
        var item = AEItems.WIRELESS_CRAFTING_TERMINAL.asItem();
        helper.assertTrue(item instanceof ItemWCT,
                "AEItemsMixin did not take: ae2:wireless_crafting_terminal is a " + item.getClass().getName());
        helper.succeed();
    }

    /** All five of this mod's items reached the ITEM registry under the expected ids. */
    @GameTest
    public void itemsAreRegistered(GameTestHelper helper) {
        for (var name : new String[] { "wireless_pattern_encoding_terminal", "wireless_pattern_access_terminal",
                "wireless_universal_terminal", "quantum_bridge_card", "magnet_card" }) {
            var id = AE2wtlibAPI.id(name);
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "Item not registered: " + id);
        }
        helper.succeed();
    }

    /** All five menu types reached the MENU registry (NeoForge does this from RegisterEvent(MENU)). */
    @GameTest
    public void menusAreRegistered(GameTestHelper helper) {
        for (var type : new net.minecraft.world.inventory.MenuType<?>[] { WCTMenu.TYPE, WETMenu.TYPE, WATMenu.TYPE,
                MagnetMenu.TYPE, TrashMenu.TYPE }) {
            helper.assertTrue(BuiltInRegistries.MENU.getKey(type) != null, "Menu type not registered: " + type);
        }
        helper.succeed();
    }

    /** The three terminals registered themselves through {@code AddTerminalEvent}. */
    @GameTest
    public void terminalsAreRegistered(GameTestHelper helper) {
        var names = new ArrayList<String>();
        for (var definition : WTDefinition.wirelessTerminals())
            names.add(definition.terminalName());
        helper.assertTrue(names.contains("crafting"), "crafting terminal missing, got " + names);
        helper.assertTrue(names.contains("pattern_encoding"), "pattern_encoding terminal missing, got " + names);
        helper.assertTrue(names.contains("pattern_access"), "pattern_access terminal missing, got " + names);
        helper.succeed();
    }

    /** The three AE2 hotkey actions were registered before AE2 finalized its key mappings. */
    @GameTest
    public void hotkeyActionsAreRegistered(GameTestHelper helper) {
        for (var name : new String[] { "ae2wtlib_restock", "ae2wtlib_magnet", "ae2wtlib_stow" }) {
            var actions = HotkeyActions.REGISTRY.get(name);
            helper.assertTrue(actions != null && !actions.isEmpty(), "Hotkey action missing: " + name);
        }
        // AE2's own wireless-terminal hotkey must still carry our crafting terminal.
        var wireless = HotkeyActions.REGISTRY.get(HotkeyAction.WIRELESS_TERMINAL);
        helper.assertTrue(wireless != null && !wireless.isEmpty(), "AE2's wireless_terminal hotkey has no actions");
        helper.succeed();
    }

    /**
     * ⚠ <strong>R14 regression guard.</strong> Building the tab contents eagerly during mod init crashed the dedicated
     * server on 26.1 ("Components not bound yet"); the fix moved it into the display-items generator. This asserts the
     * generator both runs and produces the expected stacks — i.e. neither regression (crash, or a silently empty tab)
     * can come back unnoticed.
     */
    @GameTest
    public void creativeTabIsPopulatedLazily(GameTestHelper helper) {
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(AE2wtlibAPI.id("main"));
        helper.assertTrue(tab != null, "Creative tab ae2wtlib:main is not registered");

        // Force the generator to run here, on the server, exactly as the client would after a resource reload.
        var level = helper.getLevel();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true,
                level.registryAccess()));
        var collected = new ArrayList<>(tab.getDisplayItems());
        // 3 terminals x 2 (empty + charged) + 2 WUT variants + 2 cards
        helper.assertTrue(collected.size() >= 10,
                "Creative tab produced only " + collected.size() + " stacks - expected at least 10");
        helper.assertTrue(collected.stream().anyMatch(s -> s.getItem() == AE2wtlibItems.MAGNET_CARD.asItem()),
                "Creative tab is missing the magnet card");
        helper.assertTrue(collected.stream().anyMatch(s -> s.getItem() instanceof ItemWCT),
                "Creative tab is missing the wireless crafting terminal");
        helper.succeed();
    }

    /**
     * Playbook Part 10. {@code PayloadTypeRegistry} is write-only (it exposes no lookup), so the observable server-side
     * proof that the three C2S payloads were wired is their global receivers. The S2C direction and the actual wire
     * format are covered by {@link NetworkCodecTests} and by the real-network join gate.
     */
    @GameTest
    public void serverboundReceiversAreRegistered(GameTestHelper helper) {
        var receivers = ServerPlayNetworking.getGlobalReceivers();
        for (var id : new CustomPacketPayload.Type<?>[] { CycleTerminalPacket.ID, SelectTerminalPacket.ID,
                TerminalSettingsPacket.ID }) {
            helper.assertTrue(receivers.contains(id.id()), "C2S receiver not registered: " + id.id());
        }
        helper.succeed();
    }

    /** Both custom recipe serializers reached the registry under their expected ids. */
    @GameTest
    public void recipeSerializersAreRegistered(GameTestHelper helper) {
        for (var name : new String[] { "upgrade", "combine" }) {
            helper.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(AE2wtlibAPI.id(name)),
                    "Recipe serializer not registered: " + AE2wtlibAPI.id(name));
        }
        helper.succeed();
    }

    /**
     * Registration is driven by AE2's {@code ae2:registration} entrypoint, NOT by the fork-private TAIL mixin on
     * {@code AppEngFabric#init} this port used to carry. Every other test in this class only proves that registration
     * happened <em>somehow</em>; this one pins <em>how</em>, so a silently reintroduced mixin (or a dropped entrypoint
     * declaration that happens to still work because some other ordering saved us) is caught.
     */
    @GameTest
    public void registrationRunsFromTheAe2Entrypoint(GameTestHelper helper) {
        var declared = FabricLoader.getInstance()
                .getEntrypointContainers(AE2FabricRegistration.ENTRYPOINT, AE2FabricRegistration.class)
                .stream()
                .filter(c -> c.getProvider().getMetadata().getId().equals("ae2wtlib"))
                .map(c -> c.getEntrypoint().getClass())
                .toList();
        helper.assertTrue(declared.contains(AE2wtlibRegistration.class),
                "ae2wtlib does not declare AE2wtlibRegistration under the " + AE2FabricRegistration.ENTRYPOINT
                        + " entrypoint; registration order would be unpinned (playbook Part 10)");

        try {
            Class.forName("de.mari_023.ae2wtlib.fabric.mixin.AppEngFabricMixin");
            helper.fail("AppEngFabricMixin is back: the entrypoint made it redundant and two drivers would fight "
                    + "over AE2wtlibFabric.init()");
        } catch (ClassNotFoundException expected) {
            // good
        }
        helper.succeed();
    }

    /** Sanity: the creative tab exists as a registry entry with a working icon supplier. */
    @GameTest
    public void creativeTabIconResolves(GameTestHelper helper) {
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(AE2wtlibAPI.id("main"));
        helper.assertTrue(tab != null, "Creative tab ae2wtlib:main is not registered");
        CreativeModeTab nonNull = tab;
        helper.assertTrue(!nonNull.getIconItem().isEmpty(), "Creative tab icon stack is empty");
        helper.succeed();
    }
}
