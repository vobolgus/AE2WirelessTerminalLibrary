package de.mari_023.ae2wtlib;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Loader-neutral seam for the NeoForge <em>extension methods</em> the shared tree calls on vanilla types.
 * <p>
 * <strong>Seam group 10 - not in the W1 inventory.</strong> These call sites carry no {@code net.neoforged.*} import
 * (NeoForge patches the methods straight into {@code ItemStack} / {@code Entity}), so the W1 import scan could not see
 * them; they only surfaced once the shared tree actually compiled against the Fabric classpath. Each method below
 * documents the NeoForge behaviour it replaces and what the Fabric answer is.
 */
public interface Ae2wtlibItemHooks {
    /**
     * NeoForge {@code IItemStackExtension#isNotReplaceableByPickAction}: lets an item veto being swapped out by the
     * "stow" hotkey. The default is {@code false}; only a handful of mods override it.
     * <p>
     * Fabric has no equivalent hook, so the Fabric implementation answers {@code false} - i.e. the vanilla-equivalent
     * "everything may be stowed". Behaviour parity with NeoForge for vanilla items is exact.
     */
    boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int slot);

    /**
     * NeoForge {@code IItemStackExtension#canEquip(EquipmentSlot, Entity)}: whether the stack can go into the given
     * equipment slot. NeoForge's default delegates to {@code entity.getEquipmentSlotForItem(stack) == slot}, which is
     * exactly what the Fabric implementation does.
     */
    boolean canEquip(ItemStack stack, EquipmentSlot slot, Player player);

    /**
     * NeoForge {@code IAttachmentHolder#getPersistentData()} + the community-convention {@code PreventRemoteMovement}
     * tag, used by the magnet card to leave items alone that another mod has pinned in place.
     * <p>
     * Fabric has no per-entity persistent NBT, so this answers {@code false} there: nothing can currently opt out of
     * the magnet on Fabric. That is an interop nicety, not a behaviour of this mod - revisit if a Fabric-side
     * convention emerges.
     */
    boolean preventsRemoteMovement(ItemEntity entity);

    static Ae2wtlibItemHooks get() {
        var instance = Holder.INSTANCE;
        if (instance == null)
            throw new IllegalStateException("The loader-specific Ae2wtlibItemHooks have not been initialized yet");
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(Ae2wtlibItemHooks hooks) {
        if (Holder.INSTANCE != null)
            throw new IllegalStateException("Ae2wtlibItemHooks have already been initialized");
        Holder.INSTANCE = hooks;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile Ae2wtlibItemHooks INSTANCE;

        private Holder() {}
    }
}
