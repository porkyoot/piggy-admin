package is.pig.minecraft.admin.util;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages fire ownership to attribute blame for fire propagation and burnt blocks.
 * <p>
 * This manager uses an in-memory map to track the "owner" (player UUID) of fire blocks.
 * When fire spreads, the new fire block inherits the owner of the source block.
 */
public class FireBlameManager {
    private static final Map<Long, UUID> fireOwners = new ConcurrentHashMap<>();

    /**
     * Records the owner of a fire block at the given position.
     *
     * @param pos   The position of the fire.
     * @param owner The UUID of the player who started it.
     */
    public static void setOwner(BlockPos pos, UUID owner) {
        if (owner == null) return;
        fireOwners.put(pos.asLong(), owner);
    }

    /**
     * Gets the owner attributed to a fire at the given position.
     *
     * @param pos The position to check.
     * @return The UUID of the player, or null if unknown.
     */
    public static UUID getOwner(BlockPos pos) {
        return fireOwners.get(pos.asLong());
    }

    /**
     * Removes the ownership record for a specific position.
     *
     * @param pos The position to remove.
     */
    public static void remove(BlockPos pos) {
        fireOwners.remove(pos.asLong());
    }

    /**
     * Transfers blame from one position to another (propagation).
     *
     * @param from The source fire position.
     * @param to   The new fire position.
     */
    public static void propagate(BlockPos from, BlockPos to) {
        UUID owner = getOwner(from);
        if (owner != null) {
            setOwner(to, owner);
        }
    }
}
