package de.mari_023.ae2wtlib.fabric;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.entity.player.Player;

import de.mari_023.ae2wtlib.attachment.Ae2wtlibAttachments;
import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;

/**
 * Fabric implementation of the {@link Ae2wtlibAttachments} seam.
 * <p>
 * NeoForge's {@code ct_handler} attachment is transient (no {@code .serialize(...)}, not synced) and is created per
 * player entity by a default supplier, so a weak map keyed on the player entity reproduces it exactly - including the
 * reset on respawn / dimension change, where the game creates a new {@code ServerPlayer}. Crib: AE2's
 * {@code appeng.fabric.FabricPlayerCtrlAttachment}.
 * <p>
 * The map holds both the client-side and the server-side player entity; that is also true of the NeoForge attachment.
 */
public final class FabricAttachments implements Ae2wtlibAttachments {
    private final Map<Player, CraftingTerminalHandler> handlers = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public CraftingTerminalHandler getCraftingTerminalHandler(Player player) {
        return handlers.computeIfAbsent(player, CraftingTerminalHandler::new);
    }
}
