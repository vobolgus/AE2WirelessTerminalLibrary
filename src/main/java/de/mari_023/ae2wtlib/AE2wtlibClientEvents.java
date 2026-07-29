package de.mari_023.ae2wtlib;

import net.minecraft.client.Minecraft;

import de.mari_023.ae2wtlib.api.Ae2wtlibNet;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;
import de.mari_023.ae2wtlib.networking.CycleTerminalPacket;
import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;

/**
 * The loader-neutral bodies of the two client-side behaviours that used to live directly in {@link AE2wtlibClient}
 * (which is a {@code @Mod(dist = CLIENT)} class and therefore a NeoForge-only file).
 * <p>
 * <strong>W3.</strong> Both loaders now call into here:
 * <ul>
 * <li>NeoForge: {@code AE2wtlibForge}'s {@code ClientTickEvent.Post} / {@code InputEvent.MouseScrollingEvent} handlers,
 * via the (unchanged) {@link AE2wtlibClient} entry points.</li>
 * <li>Fabric: {@code ClientTickEvents.END_CLIENT_TICK} and
 * {@code de.mari_023.ae2wtlib.fabric.mixin.client.MouseHandlerMixin}.</li>
 * </ul>
 * <p>
 * ⚠ <strong>Client-only class in the shared source set.</strong> Like {@code GuiMixin}, this references
 * {@link Minecraft} and must never be loaded on a dedicated server. It is only reachable from client entry points and
 * client mixins; the dedicated {@code runServer} boot is the guard (playbook golden rule #3).
 */
public final class AE2wtlibClientEvents {
    private AE2wtlibClientEvents() {}

    /**
     * NeoForge: {@code ClientTickEvent.Post}. Fabric: {@code ClientTickEvents.END_CLIENT_TICK}.
     * <p>
     * Both fire once per client tick after the tick body, with no ordering or cancellation semantics involved, so the
     * fabric-api callback is an exact match for the NeoForge event.
     */
    public static void clientTick() {
        if (Minecraft.getInstance().player == null)
            return;
        CraftingTerminalHandler.getCraftingTerminalHandler(Minecraft.getInstance().player).checkTerminal();
    }

    /**
     * Shift+scroll cycles the active terminal of a held Wireless Universal Terminal.
     * <p>
     * NeoForge: {@code InputEvent.MouseScrollingEvent}, cancelled when it applies. Fabric has no scroll event at all,
     * so the caller is a cancellable mixin on {@code MouseHandler#onScroll} injected at exactly the instruction where
     * NeoForge fires its event (verified by javap against both jars, W3).
     *
     * @param scrollDeltaY the vertical scroll delta; only its sign and zero-ness are used
     * @return {@code true} if the scroll was consumed and the caller must cancel further handling
     */
    public static boolean mouseScroll(double scrollDeltaY) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isShiftKeyDown() || scrollDeltaY == 0)
            return false;

        if (!(player.getMainHandItem().getItem() instanceof ItemWUT)
                && !(player.getOffhandItem().getItem() instanceof ItemWUT))
            return false;

        Ae2wtlibNet.get().sendToServer(new CycleTerminalPacket(scrollDeltaY < 0));
        return true;
    }
}
