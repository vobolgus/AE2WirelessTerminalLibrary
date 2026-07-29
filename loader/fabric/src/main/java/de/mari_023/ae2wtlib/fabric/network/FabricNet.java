package de.mari_023.ae2wtlib.fabric.network;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import de.mari_023.ae2wtlib.api.Ae2wtlibNet;

/**
 * Fabric implementation of the {@link Ae2wtlibNet} seam.
 * <p>
 * Server-to-client sends go through {@link ServerPlayNetworking}. Client-to-server sends must go through Fabric's
 * client-only {@code ClientPlayNetworking}, which may not be referenced from the main (server-safe) source set, so the
 * client entrypoint injects a sender via {@link #setClientPacketSender}. Same arrangement as AE2's
 * {@code appeng.fabric.network.FabricNetworkAdapter}.
 */
public final class FabricNet implements Ae2wtlibNet {
    @Nullable
    private static volatile Consumer<CustomPacketPayload> clientPacketSender;

    /**
     * Injected by the client entrypoint; lives here so the main source set stays free of client classes.
     */
    public static void setClientPacketSender(Consumer<CustomPacketPayload> sender) {
        clientPacketSender = sender;
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        var sender = clientPacketSender;
        if (sender == null)
            throw new IllegalStateException(
                    "Cannot send a serverbound payload: AE2WTLib's Fabric client networking has not been initialized");
        sender.accept(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
