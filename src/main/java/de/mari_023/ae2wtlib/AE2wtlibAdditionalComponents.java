package de.mari_023.ae2wtlib;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.component.DataComponentType;

import de.mari_023.ae2wtlib.api.AE2wtlibComponents;
import de.mari_023.ae2wtlib.api.EnumStreamCodec;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode;

public class AE2wtlibAdditionalComponents {
    public static final DataComponentType<MagnetMode> MAGNET_SETTINGS = AE2wtlibComponents.register("magnet_settings",
            builder -> builder.persistent(Codec.BYTE.comapFlatMap(b -> DataResult.success(MagnetMode.fromByte(b)),
                    MagnetMode::getId))
                    .networkSynchronized(EnumStreamCodec.of(MagnetMode.class)));

    // was: public static final Supplier<AttachmentType<CraftingTerminalHandler>> CT_HANDLER =
    // AE2wtlib.ATTACHMENT_TYPES.register("ct_handler", () -> AttachmentType.builder(...).build());
    // The attachment moved behind the de.mari_023.ae2wtlib.attachment.Ae2wtlibAttachments seam; the only consumer was
    // CraftingTerminalHandler#getCraftingTerminalHandler.

    @SuppressWarnings("EmptyMethod")
    public static void init() {}
}
