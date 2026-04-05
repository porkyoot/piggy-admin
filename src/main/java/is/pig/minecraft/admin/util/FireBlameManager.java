package is.pig.minecraft.admin.util;

import is.pig.minecraft.admin.storage.BlameData;
import is.pig.minecraft.admin.storage.HistoryManager;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

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
     * Records a potential griefing incident involving fire and attributes blame.
     * @param player the player responsible
     * @param pos the block position
     * @param action a description of the action (e.g. "used Fire Charge")
     */
    public static void recordFire(ServerPlayer player, BlockPos pos, String action) {
        setOwner(pos, player.getUUID());
        
        String worldId = player.serverLevel().dimension().location().toString();
        BlameData blame = new BlameData(player.getUUID(), player.getName().getString(), action, worldId, pos);
        
        // Log to persistent JSON history
        HistoryManager.logFire(player, blame);
        
        // Emit structured telemetry event
        String blockPosStr = String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
        String playerPosStr = String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ());
        
        is.pig.minecraft.admin.telemetry.HazardousPlacementEvent event = new is.pig.minecraft.admin.telemetry.HazardousPlacementEvent(
                player.getName().getString(),
                "Fire",
                blockPosStr,
                worldId,
                playerPosStr,
                player.getServer().getTickCount(),
                is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.ARSON
        );
        is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance().dispatch(event);
        is.pig.minecraft.admin.util.AdminNotifier.broadcastAdminEvent(event);
    }

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
