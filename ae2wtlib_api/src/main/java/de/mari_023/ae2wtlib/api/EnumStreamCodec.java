package de.mari_023.ae2wtlib.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Loader-neutral replacement for NeoForge's {@code NeoForgeStreamCodecs#enumCodec(Class)}.
 * <p>
 * <strong>Wire format is byte-identical to NeoForge's helper.</strong> Verified by decompiling
 * {@code NeoForgeStreamCodecs$3} out of {@code neoforge-26.1.2.87-universal.jar}: it is an anonymous
 * {@link StreamCodec} whose {@code decode}/{@code encode} delegate straight to {@link FriendlyByteBuf#readEnum(Class)}
 * / {@link FriendlyByteBuf#writeEnum(Enum)} — i.e. a VarInt of the constant's ordinal. This class does exactly the
 * same, so switching both loaders over to it does not change a single byte on the wire (playbook Part 10: packet
 * formats must be pinned, and both loader builds must agree).
 * <p>
 * Note that ordinals are <em>not</em> registry ids, so this codec is not affected by the 26.1 raw-registry-id sync gap;
 * it is only sensitive to the enum's declaration order, which is compiled into both sides.
 */
public final class EnumStreamCodec {
    private EnumStreamCodec() {}

    public static <B extends FriendlyByteBuf, V extends Enum<V>> StreamCodec<B, V> of(Class<V> enumClass) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buffer) {
                return buffer.readEnum(enumClass);
            }

            @Override
            public void encode(B buffer, V value) {
                buffer.writeEnum(value);
            }
        };
    }
}
