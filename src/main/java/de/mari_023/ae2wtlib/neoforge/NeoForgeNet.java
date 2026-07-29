package de.mari_023.ae2wtlib.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import de.mari_023.ae2wtlib.api.Ae2wtlibNet;

/**
 * NeoForge implementation of the {@link Ae2wtlibNet} seam - a direct 1:1 mapping onto the two distributors upstream
 * used inline.
 */
public final class NeoForgeNet implements Ae2wtlibNet {
    @Override
    @OnlyIn(Dist.CLIENT)
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
