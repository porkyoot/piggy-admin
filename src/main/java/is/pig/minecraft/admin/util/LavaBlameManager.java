package is.pig.minecraft.admin.util;

import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lava ownership to attribute blame for lava-triggered fires and damage.
 */
public class LavaBlameManager {
    private static final Map<Long, UUID> lavaOwners = new ConcurrentHashMap<>();

    /**
     * Records a potential griefing incident involving lava and attributes blame.
     * @param player the player responsible
     * @param pos the block position
     */
    public static void recordLava(ServerPlayer player, BlockPos pos) {
        setOwner(pos, player.getUUID());
        
        String worldId = player.serverLevel().dimension().location().toString();
        
        // Log to persistent JSON history
        HistoryManager.logLava(player, pos);
        
        // Emit structured telemetry event
        String blockPosStr = String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
        String playerPosStr = String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ());
        
        is.pig.minecraft.admin.telemetry.HazardousPlacementEvent event = new is.pig.minecraft.admin.telemetry.HazardousPlacementEvent(
                player.getName().getString(),
                "Lava",
                blockPosStr,
                worldId,
                playerPosStr,
                player.getServer().getTickCount(),
                is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.HAZARD
        );
        StructuredEventDispatcher.getInstance().dispatch(event);
        AdminNotifier.broadcastAdminEvent(event);
    }

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
