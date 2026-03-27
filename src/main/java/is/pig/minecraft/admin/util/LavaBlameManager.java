package is.pig.minecraft.admin.util;

import net.minecraft.core.BlockPos;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lava ownership to attribute blame for lava-triggered fires and damage.
 */
/**
 * Centralized registry for tracking lava block ownership.
 * Allows the server to attribute blame for fires or damage caused by lava
 * that was placed or manipulated by a player.
 */
public class LavaBlameManager {
    private static final Map<Long, UUID> lavaOwners = new ConcurrentHashMap<>();

    public static void setOwner(BlockPos pos, UUID owner) {
        if (owner == null) return;
        lavaOwners.put(pos.asLong(), owner);
    }

    public static UUID getOwner(BlockPos pos) {
        return lavaOwners.get(pos.asLong());
    }

    public static void remove(BlockPos pos) {
        lavaOwners.remove(pos.asLong());
    }

    /**
     * Propagates ownership from a source block to a destination block.
     * Typically used when lava flows from one position to another.
     * 
     * @param from The source block position (flowing from).
     * @param to The target block position (flowing to).
     */
    public static void propagate(BlockPos from, BlockPos to) {
        UUID owner = getOwner(from);
        if (owner != null) {
            setOwner(to, owner);
        }
    }
}
