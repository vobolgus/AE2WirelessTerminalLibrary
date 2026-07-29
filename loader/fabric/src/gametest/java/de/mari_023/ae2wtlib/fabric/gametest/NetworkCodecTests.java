package de.mari_023.ae2wtlib.fabric.gametest;

import java.util.HashMap;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import appeng.menu.locator.MenuLocators;

import de.mari_023.ae2wtlib.AE2wtlibItems;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.networking.CycleTerminalPacket;
import de.mari_023.ae2wtlib.networking.RestockAmountPacket;
import de.mari_023.ae2wtlib.networking.SelectTerminalPacket;
import de.mari_023.ae2wtlib.networking.TerminalSettingsPacket;
import de.mari_023.ae2wtlib.networking.UpdateRestockPacket;
import de.mari_023.ae2wtlib.networking.UpdateWUTPackage;

/**
 * ⚠ <strong>R5 — the headless half of the multiplayer guard (playbook Part 10).</strong>
 * <p>
 * Singleplayer and every other gametest use a memory connection, which short-circuits serialization entirely, so a
 * broken {@code StreamCodec} is invisible until a real TCP join. These tests push each of the six payloads through its
 * own {@code STREAM_CODEC} into a real {@link RegistryFriendlyByteBuf} and read it back, asserting the value survives
 * <em>and</em> that the buffer is fully consumed — a codec that writes more than it reads is exactly the stream-desync
 * that cascades into "Invalid tag id" garbage on a live server.
 * <p>
 * This does not replace the real-network join (it cannot catch raw-id divergence between two differently-modded sides),
 * but it catches every self-inconsistent codec for free, on every run.
 */
public class NetworkCodecTests {
    private <T> T roundTrip(GameTestHelper helper, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T value,
            String what) {
        var registries = helper.getLevel().registryAccess();
        var out = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        codec.encode(out, value);

        int written = out.writerIndex();
        helper.assertTrue(written > 0, what + ": codec wrote 0 bytes");

        var decoded = codec.decode(out);
        helper.assertTrue(out.readerIndex() == written,
                what + ": codec read " + out.readerIndex() + " of " + written + " bytes - stream desync");
        return decoded;
    }

    @GameTest
    public void cycleTerminalPacketRoundTrips(GameTestHelper helper) {
        for (var flag : new boolean[] { true, false }) {
            var original = new CycleTerminalPacket(flag);
            var decoded = roundTrip(helper, CycleTerminalPacket.STREAM_CODEC.mapStream(b -> b), original,
                    "CycleTerminalPacket");
            helper.assertValueEqual(decoded.isRightClick(), original.isRightClick(),
                    "CycleTerminalPacket.isRightClick");
        }
        helper.succeed();
    }

    @GameTest
    public void selectTerminalPacketRoundTrips(GameTestHelper helper) {
        for (var definition : WTDefinition.wirelessTerminals()) {
            var original = new SelectTerminalPacket(definition);
            var decoded = roundTrip(helper, SelectTerminalPacket.STREAM_CODEC.mapStream(b -> b), original,
                    "SelectTerminalPacket(" + definition.terminalName() + ")");
            helper.assertValueEqual(decoded.terminal().terminalName(), definition.terminalName(),
                    "SelectTerminalPacket.terminal");
        }
        helper.succeed();
    }

    @GameTest
    public void terminalSettingsPacketRoundTrips(GameTestHelper helper) {
        var original = new TerminalSettingsPacket(MenuLocators.forInventorySlot(3), true, false, true, false, true);
        var decoded = roundTrip(helper, TerminalSettingsPacket.STREAM_CODEC, original, "TerminalSettingsPacket");

        helper.assertValueEqual(decoded.pickBlock(), original.pickBlock(), "TerminalSettingsPacket.pickBlock");
        helper.assertValueEqual(decoded.restock(), original.restock(), "TerminalSettingsPacket.restock");
        helper.assertValueEqual(decoded.magnet(), original.magnet(), "TerminalSettingsPacket.magnet");
        helper.assertValueEqual(decoded.pickupToME(), original.pickupToME(), "TerminalSettingsPacket.pickupToME");
        helper.assertValueEqual(decoded.craftIfMissing(), original.craftIfMissing(),
                "TerminalSettingsPacket.craftIfMissing");
        helper.assertValueEqual(decoded.terminal(), original.terminal(), "TerminalSettingsPacket.terminal");
        helper.succeed();
    }

    @GameTest
    public void updateRestockPacketRoundTrips(GameTestHelper helper) {
        var stack = new ItemStack(AE2wtlibItems.MAGNET_CARD, 3);
        var original = new UpdateRestockPacket(7, stack);
        var decoded = roundTrip(helper, UpdateRestockPacket.STREAM_CODEC, original, "UpdateRestockPacket");

        helper.assertValueEqual(decoded.slot(), original.slot(), "UpdateRestockPacket.slot");
        helper.assertTrue(ItemStack.matches(decoded.itemStack(), original.itemStack()),
                "UpdateRestockPacket.itemStack did not survive: " + decoded.itemStack());
        helper.succeed();
    }

    /** Also covers the empty-stack case, which uses the OPTIONAL stream codec's null branch. */
    @GameTest
    public void updateRestockPacketRoundTripsEmptyStack(GameTestHelper helper) {
        var original = new UpdateRestockPacket(-1, ItemStack.EMPTY);
        var decoded = roundTrip(helper, UpdateRestockPacket.STREAM_CODEC, original, "UpdateRestockPacket(empty)");
        helper.assertValueEqual(decoded.slot(), -1, "UpdateRestockPacket.slot");
        helper.assertTrue(decoded.itemStack().isEmpty(), "UpdateRestockPacket lost the empty-stack marker");
        helper.succeed();
    }

    @GameTest
    public void updateWutPackageRoundTrips(GameTestHelper helper) {
        // A WUT that has had a terminal merged in carries a non-trivial component patch - the interesting case.
        var wut = new ItemStack(AE2wtlibItems.UNIVERSAL_TERMINAL);
        var toMerge = new ItemStack(AE2wtlibItems.PATTERN_ACCESS_TERMINAL);
        var merged = de.mari_023.ae2wtlib.wut.recipe.Common.mergeTerminal(wut, toMerge, WTDefinition.of(toMerge));

        var original = new UpdateWUTPackage(MenuLocators.forInventorySlot(0), merged);
        var decoded = roundTrip(helper, UpdateWUTPackage.STREAM_CODEC, original, "UpdateWUTPackage");

        helper.assertValueEqual(decoded.locator(), original.locator(), "UpdateWUTPackage.locator");
        helper.assertValueEqual(decoded.patch(), original.patch(), "UpdateWUTPackage.patch");
        helper.succeed();
    }

    /**
     * The map payload: per-entry comparison rather than whole-buffer, per the playbook. Uses item HOLDERS, which is the
     * registry-backed part of the wire format.
     */
    @GameTest
    public void restockAmountPacketRoundTrips(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var itemRegistry = registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM);

        var items = new HashMap<Holder<Item>, Long>();
        items.put(itemRegistry.getOrThrow(Items.COBBLESTONE.builtInRegistryHolder().key()), 4096L);
        items.put(itemRegistry.getOrThrow(AE2wtlibItems.MAGNET_CARD.asItem().builtInRegistryHolder().key()), 1L);

        var original = new RestockAmountPacket(items);
        var decoded = roundTrip(helper, RestockAmountPacket.STREAM_CODEC, original, "RestockAmountPacket");

        helper.assertValueEqual(decoded.items().size(), items.size(), "RestockAmountPacket entry count");
        items.forEach((holder, count) -> {
            var got = decoded.items().get(holder);
            helper.assertTrue(got != null, "RestockAmountPacket lost entry " + holder.getRegisteredName());
            helper.assertValueEqual(got, count, "RestockAmountPacket count for " + holder.getRegisteredName());
        });
        helper.succeed();
    }
}
