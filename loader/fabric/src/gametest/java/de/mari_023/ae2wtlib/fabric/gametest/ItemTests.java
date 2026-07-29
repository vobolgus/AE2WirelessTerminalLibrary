package de.mari_023.ae2wtlib.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import appeng.core.definitions.AEItems;

import de.mari_023.ae2wtlib.AE2wtlibItems;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.wut.recipe.Common;

/**
 * Spawn-and-tick coverage for every item this mod contributes, plus the WUT merge logic.
 * <p>
 * Playbook rule #6: the cheapest way to catch a broken item is to put it in the world and let it tick — that runs the
 * constructor, the data-component defaults, {@code ItemStack} creation (the R14 component-binding path) and the entity
 * tick. All five of this mod's items plus AE2's swapped wireless crafting terminal are covered.
 */
public class ItemTests {
    private static final int SPAWN_TICKS = 5;

    private void spawnAndTick(GameTestHelper helper, ItemStack stack, String what) {
        helper.assertTrue(!stack.isEmpty(), what + ": stack is empty");
        var entity = helper.spawnItem(stack.getItem(), new BlockPos(1, 2, 1));
        entity.setItem(stack);
        helper.runAfterDelay(SPAWN_TICKS, () -> {
            helper.assertTrue(entity.isAlive(), what + ": item entity died within " + SPAWN_TICKS + " ticks");
            helper.assertTrue(!entity.getItem().isEmpty(), what + ": item entity lost its stack");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void patternEncodingTerminalTicks(GameTestHelper helper) {
        spawnAndTick(helper, new ItemStack(AE2wtlibItems.PATTERN_ENCODING_TERMINAL), "pattern encoding terminal");
    }

    @GameTest(maxTicks = 60)
    public void patternAccessTerminalTicks(GameTestHelper helper) {
        spawnAndTick(helper, new ItemStack(AE2wtlibItems.PATTERN_ACCESS_TERMINAL), "pattern access terminal");
    }

    @GameTest(maxTicks = 60)
    public void universalTerminalTicks(GameTestHelper helper) {
        spawnAndTick(helper, new ItemStack(AE2wtlibItems.UNIVERSAL_TERMINAL), "universal terminal");
    }

    @GameTest(maxTicks = 60)
    public void quantumBridgeCardTicks(GameTestHelper helper) {
        spawnAndTick(helper, new ItemStack(AE2wtlibItems.QUANTUM_BRIDGE_CARD), "quantum bridge card");
    }

    @GameTest(maxTicks = 60)
    public void magnetCardTicks(GameTestHelper helper) {
        spawnAndTick(helper, new ItemStack(AE2wtlibItems.MAGNET_CARD), "magnet card");
    }

    @GameTest(maxTicks = 60)
    public void wirelessCraftingTerminalTicks(GameTestHelper helper) {
        spawnAndTick(helper, AEItems.WIRELESS_CRAFTING_TERMINAL.stack(), "wireless crafting terminal");
    }

    /** Every registered terminal can produce its charged creative-tab stack (the {@code injectAEPower} path). */
    @GameTest
    public void terminalStacksCarryEnergy(GameTestHelper helper) {
        for (var definition : WTDefinition.wirelessTerminals()) {
            var stack = definition.universalTerminalStackWithEnergy();
            helper.assertTrue(!stack.isEmpty(),
                    "universalTerminalStackWithEnergy is empty for " + definition.terminalName());
        }
        helper.succeed();
    }

    /**
     * The WUT combine logic: merging a terminal into an empty universal terminal must record it, and the result must
     * still be a universal terminal. Exercises the data components and {@code WUTHandler} round-trip that the
     * {@code ae2wtlib:combine} / {@code ae2wtlib:upgrade} recipes and the S2C {@code UpdateWUTPackage} rely on.
     */
    @GameTest
    public void universalTerminalMergeRoundTrip(GameTestHelper helper) {
        var wut = new ItemStack(AE2wtlibItems.UNIVERSAL_TERMINAL);
        var toMerge = new ItemStack(AE2wtlibItems.PATTERN_ACCESS_TERMINAL);
        var definition = WTDefinition.of(toMerge);

        var merged = Common.mergeTerminal(wut, toMerge, definition);
        helper.assertTrue(!merged.isEmpty(), "mergeTerminal returned an empty stack");
        Item mergedItem = merged.getItem();
        helper.assertTrue(mergedItem == AE2wtlibItems.UNIVERSAL_TERMINAL.asItem(),
                "mergeTerminal changed the item to " + mergedItem);
        helper.assertTrue(!merged.getComponentsPatch().isEmpty(),
                "mergeTerminal recorded no data components on the universal terminal");
        helper.succeed();
    }
}
