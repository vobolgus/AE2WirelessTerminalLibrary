package de.mari_023.ae2wtlib.attachment;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Player;

import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;

/**
 * Loader-neutral seam for the per-player {@link CraftingTerminalHandler}, replacing NeoForge's data attachment
 * ({@code AttachmentType} + {@code NeoForgeRegistries.ATTACHMENT_TYPES} + {@code player.getData(...)}).
 * <p>
 * The attachment is <strong>transient</strong> upstream - {@code AttachmentType.builder(...)} without
 * {@code .serialize(...)} means it is neither persisted nor synced, and a fresh handler is created per player entity
 * (so respawns and dimension changes reset it). Both implementations must keep exactly that lifetime; on Fabric a weak
 * map is the smallest faithful shape (crib: AE2's {@code appeng.fabric.FabricPlayerCtrlAttachment}).
 */
public interface Ae2wtlibAttachments {
    /**
     * @return the handler attached to the given player, creating it on first access.
     */
    CraftingTerminalHandler getCraftingTerminalHandler(Player player);

    static Ae2wtlibAttachments get() {
        var instance = Holder.INSTANCE;
        if (instance == null)
            throw new IllegalStateException("The loader-specific Ae2wtlibAttachments has not been initialized yet");
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(Ae2wtlibAttachments attachments) {
        if (Holder.INSTANCE != null)
            throw new IllegalStateException("Ae2wtlibAttachments has already been initialized");
        Holder.INSTANCE = attachments;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile Ae2wtlibAttachments INSTANCE;

        private Holder() {}
    }
}
