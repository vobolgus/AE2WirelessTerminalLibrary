package de.mari_023.ae2wtlib.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import de.mari_023.ae2wtlib.networking.AE2wtlibPacket;
import de.mari_023.ae2wtlib.networking.CycleTerminalPacket;
import de.mari_023.ae2wtlib.networking.RestockAmountPacket;
import de.mari_023.ae2wtlib.networking.SelectTerminalPacket;
import de.mari_023.ae2wtlib.networking.TerminalSettingsPacket;
import de.mari_023.ae2wtlib.networking.UpdateRestockPacket;
import de.mari_023.ae2wtlib.networking.UpdateWUTPackage;

/**
 * Registers AE2WTLib's six payload <em>types</em> and the server-side receivers. The Fabric twin of the
 * {@code RegisterPayloadHandlersEvent} block in {@code AE2wtlibForge}.
 * <p>
 * <strong>Playbook Part 10 discipline.</strong> Payload types are addressed by {@code CustomPacketPayload.Type}, i.e.
 * by {@code Identifier} - never by a raw registry id - and the {@code STREAM_CODEC}s are the <em>shared</em> ones, so
 * the wire format is byte-for-byte what the NeoForge build produces. Registration order below deliberately mirrors
 * {@code AE2wtlibForge} (C2S: cycle, select, settings; S2C: wut, restock, amounts); it does not affect the wire format,
 * but keeping it identical makes the two loaders diffable.
 * <p>
 * The client receivers live in {@code AE2wtlibFabricClient} - {@code ClientPlayNetworking} is a client-only class.
 */
public final class FabricNetworkInit {
    private FabricNetworkInit() {}

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(CycleTerminalPacket.ID, cast(CycleTerminalPacket.STREAM_CODEC));
        PayloadTypeRegistry.serverboundPlay().register(SelectTerminalPacket.ID,
                cast(SelectTerminalPacket.STREAM_CODEC));
        PayloadTypeRegistry.serverboundPlay().register(TerminalSettingsPacket.ID,
                cast(TerminalSettingsPacket.STREAM_CODEC));

        PayloadTypeRegistry.clientboundPlay().register(UpdateWUTPackage.ID, cast(UpdateWUTPackage.STREAM_CODEC));
        PayloadTypeRegistry.clientboundPlay().register(UpdateRestockPacket.ID, cast(UpdateRestockPacket.STREAM_CODEC));
        PayloadTypeRegistry.clientboundPlay().register(RestockAmountPacket.ID, cast(RestockAmountPacket.STREAM_CODEC));
    }

    public static void registerServerReceivers() {
        registerC2S(CycleTerminalPacket.ID);
        registerC2S(SelectTerminalPacket.ID);
        registerC2S(TerminalSettingsPacket.ID);
    }

    /**
     * NeoForge's {@code registrar.playToServer(...)} wraps the handler in {@code context.enqueueWork(...)}; on Fabric
     * the equivalent is hopping to the server thread via {@code context.server().execute(...)}. Without it the payload
     * would be handled on the netty thread.
     */
    private static <T extends AE2wtlibPacket> void registerC2S(CustomPacketPayload.Type<T> id) {
        ServerPlayNetworking.registerGlobalReceiver(id, (payload, context) -> {
            // NeoForge's registrar.playToServer(...) wraps the handler in context.enqueueWork(...).
            if (context.server().isSameThread())
                payload.processPacketData(context.player());
            else
                context.server().execute(() -> payload.processPacketData(context.player()));
        });
    }

    /**
     * The packets declare their codecs over varying buffer types ({@code ByteBuf} for the trivial ones,
     * {@code RegistryFriendlyByteBuf} for those carrying item stacks). {@code PayloadTypeRegistry} wants the widest
     * one; the cast is safe because a {@code StreamCodec<? super RegistryFriendlyByteBuf, T>} accepts exactly that
     * buffer.
     */
    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> cast(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
    }
}
