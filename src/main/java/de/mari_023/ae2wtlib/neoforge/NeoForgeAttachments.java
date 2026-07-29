package de.mari_023.ae2wtlib.neoforge;

import java.util.function.Supplier;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.attachment.Ae2wtlibAttachments;
import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;

/**
 * NeoForge implementation of the {@link Ae2wtlibAttachments} seam - the {@code ct_handler} data attachment, moved here
 * verbatim from {@code AE2wtlib}/{@code AE2wtlibAdditionalComponents}.
 */
public final class NeoForgeAttachments implements Ae2wtlibAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, AE2wtlibAPI.MOD_NAME);

    public static final Supplier<AttachmentType<CraftingTerminalHandler>> CT_HANDLER = ATTACHMENT_TYPES
            .register("ct_handler",
                    () -> AttachmentType.builder((player) -> new CraftingTerminalHandler((Player) player)).build());

    @Override
    public CraftingTerminalHandler getCraftingTerminalHandler(Player player) {
        return player.getData(CT_HANDLER);
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
