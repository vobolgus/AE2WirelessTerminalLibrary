package de.mari_023.ae2wtlib.api;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-neutral seam for sending custom payloads, replacing NeoForge's {@code PacketDistributor} and
 * {@code ClientPacketDistributor}. The loader-specific implementation is injected exactly once during mod construction
 * via {@link #init}.
 * <p>
 * Modelled on AE2's {@code appeng.core.network.NetworkAdapter} (same holder/init shape), but typed to plain
 * {@link CustomPacketPayload} rather than to AE2WTLib's own {@code AE2wtlibPacket}: this seam lives in the API module,
 * which sits <em>below</em> the mod module, and it also has to carry AE2's own {@code HotkeyPacket} (sent from
 * {@code TerminalSelectionPanel} / {@code IUniversalTerminalCapable}).
 * <p>
 * <strong>Payload <em>type</em> registration is deliberately not part of this seam</strong> — each loader registers the
 * six AE2WTLib payloads itself, using the same {@code CustomPacketPayload.Type} identifiers and the same shared
 * {@code STREAM_CODEC}s, so the wire format is identical on both loaders (playbook Part 10).
 */
public interface Ae2wtlibNet {
    /**
     * Sends the given payload from the client to the server. Must only be called on the client while connected.
     */
    void sendToServer(CustomPacketPayload payload);

    /**
     * Sends the given payload to the given player.
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    static Ae2wtlibNet get() {
        var instance = Holder.INSTANCE;
        if (instance == null)
            throw new IllegalStateException("The loader-specific Ae2wtlibNet has not been initialized yet");
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(Ae2wtlibNet net) {
        if (Holder.INSTANCE != null)
            throw new IllegalStateException("Ae2wtlibNet has already been initialized");
        Holder.INSTANCE = net;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile Ae2wtlibNet INSTANCE;

        private Holder() {}
    }
}
