package de.mari_023.ae2wtlib.fabric.gametest;

import java.util.Set;
import java.util.TreeSet;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.api.Ae2wtlibAccessories;
import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.api.terminal.WUTHandler;
import de.mari_023.ae2wtlib.wut.WTDefinitions;

/**
 * Guards for the Trinkets (accessory-slot) support.
 * <p>
 * Wearing a terminal is <strong>data-driven</strong>: the code path exists as soon as {@link Ae2wtlibAccessories} has
 * an implementation, but nothing can ever be put into an accessory slot unless the item opts into the slot's item tag
 * and something attaches that slot to the player. Neither fails loudly — a missing file just means the terminal can
 * never be worn. Trinkets itself is absent from the dev runtime, so the equip flow is an in-world check; what is
 * provable headlessly is the data (vanilla builds item tags for every namespace present in the loaded packs, installed
 * or not) and that the lookup seam is wired at all.
 * <p>
 * ⚠ This class must be listed in the {@code fabric-gametest} entrypoint array of
 * {@code src/gametest/resources/fabric.mod.json} or it is silently skipped.
 */
public class AccessorySlotTests {
    /** The slot AE2 remapped Curios' generic {@code curio} slot onto (see AE2's {@code TrinketsSlots}). */
    private static final TagKey<Item> BELT_SLOT = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("trinkets", "legs/belt"));

    /**
     * Every terminal this mod registers must be opted into the accessory slot tag. Drift guard: the tag file is the
     * hand-maintained Fabric twin of {@code data/curios/tags/item/curio.json}, which is deliberately excluded from the
     * Fabric jar (Curios is NeoForge-only) — so the two cannot simply be compared at runtime, and a terminal added
     * without a tag entry would be silently un-wearable.
     */
    @GameTest
    public void everyTerminalOptsIntoTheAccessorySlot(GameTestHelper helper) {
        var tagged = collect(BELT_SLOT);
        helper.assertTrue(!tagged.isEmpty(),
                "the trinkets slot tag " + BELT_SLOT.location() + " is empty or missing: no terminal could be worn");

        for (var definition : WTDefinition.wirelessTerminals()) {
            var id = BuiltInRegistries.ITEM.getKey(definition.item());
            helper.assertTrue(tagged.contains(id), "terminal " + id + " is not in " + BELT_SLOT.location()
                    + ", so it can never be worn. Tagged: " + tagged);
        }

        var wut = BuiltInRegistries.ITEM.getKey(AE2wtlibAPI.getWUT());
        helper.assertTrue(tagged.contains(wut), "the universal terminal " + wut + " is not in " + BELT_SLOT.location());
        helper.succeed();
    }

    /**
     * ...and AE2 must be attaching that slot to the player. AE2WTLib ships no {@code entities} file of its own: it
     * hard-depends on AE2, which owns the attachment (Trinkets' own jar ships none at all, so without it the slot
     * simply does not exist on a player and both mods' tags are inert).
     */
    @GameTest
    public void ae2AttachesTheAccessorySlot(GameTestHelper helper) {
        var id = Identifier.fromNamespaceAndPath("trinkets", "entities/ae2.json");
        helper.assertTrue(helper.getLevel().getServer().getResourceManager().getResource(id).isPresent(),
                "missing " + id + ": AE2 no longer attaches an accessory slot to players, so this mod's slot tag is "
                        + "inert. Re-check the slot AE2 uses and update data/trinkets/tags/item/... to match.");
        helper.succeed();
    }

    /**
     * The lookup seam is injected, and {@code findTerminal} tolerates it returning "no accessory inventory" (which is
     * what AE2 reports with Trinkets absent, i.e. in this very run) instead of throwing.
     */
    @GameTest
    public void accessoryLookupIsWired(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();

        // With Trinkets absent AE2's seam returns null; the point is that a wired lookup answers without blowing up.
        Ae2wtlibAccessories.get(player);
        helper.assertTrue(WUTHandler.findTerminal(player, WTDefinitions.CRAFTING) == null,
                "an empty mock player must not resolve a terminal");
        helper.succeed();
    }

    private static Set<Identifier> collect(TagKey<Item> tag) {
        var ids = new TreeSet<>(java.util.Comparator.comparing(Identifier::toString));
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            ids.add(BuiltInRegistries.ITEM.getKey(holder.value()));
        }
        return ids;
    }
}
