package de.mari_023.ae2wtlib.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import team.reborn.energy.api.EnergyStorage;

import appeng.api.config.Actionable;
import appeng.core.definitions.AEItems;

import de.mari_023.ae2wtlib.AE2wtlibItems;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;

/**
 * W4 seam #7 guard: the three powered terminals must expose a Team Reborn {@link EnergyStorage} through
 * {@code EnergyStorage.ITEM}, and the wireless crafting terminal must keep AE2's own registration despite the
 * {@code AEItemsMixin} class swap.
 * <p>
 * This is the headless equivalent of "put the terminal in a charger and watch the bar move": a missing
 * {@code registerForItems} call, a wrong {@code validItem}, or a broken {@code IAEItemPowerStorage} all show up here.
 */
public class EnergyTests {
    private EnergyStorage lookup(ItemStack stack) {
        return EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
    }

    private void assertPowered(GameTestHelper helper, ItemStack stack, String what) {
        var storage = lookup(stack);
        helper.assertTrue(storage != null, what + ": no EnergyStorage registered for the item");
        helper.assertTrue(storage.getCapacity() > 0,
                what + ": EnergyStorage reports capacity " + storage.getCapacity());
        helper.assertTrue(storage.getAmount() == 0,
                what + ": a fresh terminal should be empty but reports " + storage.getAmount());
    }

    @GameTest
    public void universalTerminalExposesEnergy(GameTestHelper helper) {
        assertPowered(helper, new ItemStack(AE2wtlibItems.UNIVERSAL_TERMINAL), "universal terminal");
        helper.succeed();
    }

    @GameTest
    public void patternAccessTerminalExposesEnergy(GameTestHelper helper) {
        assertPowered(helper, new ItemStack(AE2wtlibItems.PATTERN_ACCESS_TERMINAL), "pattern access terminal");
        helper.succeed();
    }

    @GameTest
    public void patternEncodingTerminalExposesEnergy(GameTestHelper helper) {
        assertPowered(helper, new ItemStack(AE2wtlibItems.PATTERN_ENCODING_TERMINAL), "pattern encoding terminal");
        helper.succeed();
    }

    /**
     * AE2 registers the energy capability for {@code AEItems.WIRELESS_CRAFTING_TERMINAL} itself; this mod deliberately
     * does not. If {@code AEItemsMixin}'s swap ever broke the definition, the lookup would go with it — so this doubles
     * as a second R6 guard.
     */
    @GameTest
    public void wirelessCraftingTerminalKeepsAe2sEnergyCapability(GameTestHelper helper) {
        assertPowered(helper, AEItems.WIRELESS_CRAFTING_TERMINAL.stack(), "wireless crafting terminal");
        helper.succeed();
    }

    /**
     * A charged terminal must report that charge through the Fabric lookup - this is the actual AE-to-FE conversion
     * path in {@code PoweredItemEnergyStorage}, not just its presence.
     */
    @GameTest
    public void chargedTerminalReportsItsEnergy(GameTestHelper helper) {
        var stack = new ItemStack(AE2wtlibItems.UNIVERSAL_TERMINAL);
        ItemWT item = AE2wtlibItems.UNIVERSAL_TERMINAL.asItem();
        item.injectAEPower(stack, item.getAEMaxPower(stack), Actionable.MODULATE);

        var storage = lookup(stack);
        helper.assertTrue(storage != null, "charged universal terminal: no EnergyStorage registered");
        helper.assertTrue(storage.getAmount() > 0,
                "charged universal terminal reports 0 FE (AE->FE conversion is broken)");
        helper.assertTrue(storage.getAmount() <= storage.getCapacity(),
                "charged universal terminal reports more energy than its capacity");
        helper.succeed();
    }
}
