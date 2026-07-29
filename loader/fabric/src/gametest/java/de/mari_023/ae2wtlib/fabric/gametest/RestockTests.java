package de.mari_023.ae2wtlib.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import de.mari_023.ae2wtlib.AE2wtlibEvents;
import de.mari_023.ae2wtlib.AE2wtlibItems;
import de.mari_023.ae2wtlib.api.AE2wtlibComponents;
import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;

/**
 * Restock / magnet round-trips against a real {@code ServerPlayer}, without an ME network.
 *
 * <h2>Scope, honestly stated</h2>
 *
 * A full restock needs a powered, in-range ME grid, which is an in-world scenario (AE2's own testplot framework, or the
 * live server). What <em>is</em> provable headlessly — and what actually broke during this port — is the <strong>guard
 * chain</strong>: every one of the six event paths funnels into {@code AE2wtlibEvents.restock} /
 * {@code insertStackInME}, and each must bail cleanly when there is no terminal, no grid, or the feature flag is off. A
 * NullPointerException in that chain would take down the server on any player's first right-click, so this is the
 * highest-value part to pin.
 * <p>
 * These run against a mock server player from {@link GameTestHelper#makeMockServerPlayerInLevel()}.
 */
public class RestockTests {
    /** The handler must exist for any player and report "not in range" when the player carries no terminal. */
    @GameTest
    public void handlerIsSafeWithoutATerminal(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();

        var handler = CraftingTerminalHandler.getCraftingTerminalHandler(player);
        helper.assertTrue(handler != null, "no CraftingTerminalHandler for a player without a terminal");
        helper.assertFalse(handler.inRange(), "a player with no terminal reported inRange()");
        helper.assertTrue(handler.getCraftingTerminal().isEmpty(),
                "a player with no terminal reported a non-empty terminal stack");
        helper.assertFalse(handler.isRestockEnabled(), "restock reported enabled with no terminal");
        helper.succeed();
    }

    /** {@code checkTerminal()} is called every client tick and from several event paths; it must never throw. */
    @GameTest
    public void checkTerminalIsSafeWithoutATerminal(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        CraftingTerminalHandler.getCraftingTerminalHandler(player).checkTerminal();
        helper.succeed();
    }

    /**
     * The restock guard chain: with no terminal there is nothing to restock, so the stack must be untouched and the
     * setter must never fire. This is the path every one of the six event hooks takes on a vanilla player.
     */
    @GameTest
    public void restockIsANoOpWithoutATerminal(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var stack = new ItemStack(Items.COBBLESTONE, 17);
        var called = new boolean[] { false };

        AE2wtlibEvents.restock(player, stack, stack, s -> called[0] = true);

        helper.assertValueEqual(stack.getCount(), 17, "restock modified the stack without a terminal");
        helper.assertFalse(called[0], "restock invoked its setter without a terminal");
        helper.succeed();
    }

    /** Empty and creative are the two early-outs before any terminal lookup happens. */
    @GameTest
    public void restockIgnoresEmptyStacks(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var called = new boolean[] { false };

        AE2wtlibEvents.restock(player, ItemStack.EMPTY, ItemStack.EMPTY, s -> called[0] = true);

        helper.assertFalse(called[0], "restock invoked its setter for an empty stack");
        helper.succeed();
    }

    /**
     * A terminal in the inventory with restock DISABLED must still be a no-op — the component default is the switch
     * every one of the six hooks reads.
     */
    @GameTest
    public void restockRespectsTheDisabledComponent(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var terminal = new ItemStack(AE2wtlibItems.UNIVERSAL_TERMINAL);
        helper.assertFalse(terminal.getOrDefault(AE2wtlibComponents.RESTOCK, false),
                "a fresh terminal defaults to restock ENABLED - the six event hooks would fire unasked");
        player.getInventory().add(terminal);

        var stack = new ItemStack(Items.COBBLESTONE, 5);
        var called = new boolean[] { false };
        AE2wtlibEvents.restock(player, stack, stack, s -> called[0] = true);

        helper.assertValueEqual(stack.getCount(), 5, "restock modified the stack with the component disabled");
        helper.assertFalse(called[0], "restock invoked its setter with the component disabled");
        helper.succeed();
    }

    /**
     * The magnet path ({@code ItemEntityMixin} → {@code insertStackInME}) must be a clean no-op for an ordinary dropped
     * item and must NOT consume it — a bug here would silently eat items on every pickup.
     */
    @GameTest
    public void magnetIsANoOpWithoutATerminal(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var entity = helper.spawnItem(Items.COBBLESTONE, new BlockPos(1, 2, 1));
        entity.setItem(new ItemStack(Items.COBBLESTONE, 9));

        AE2wtlibEvents.insertStackInME(entity, player);

        helper.assertValueEqual(entity.getItem().getCount(), 9,
                "insertStackInME consumed items without an ME network");
        helper.succeed();
    }

    /** The pick-block path ({@code ServerGamePacketListenerImplMixin}) has the same requirement. */
    @GameTest
    public void pickBlockIsANoOpWithoutATerminal(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var before = player.getInventory().getSelectedItem().copy();

        AE2wtlibEvents.pickBlock(player, new ItemStack(Items.STONE));

        helper.assertTrue(ItemStack.matches(player.getInventory().getSelectedItem(), before),
                "pickBlock modified the hotbar without a terminal");
        helper.succeed();
    }
}
