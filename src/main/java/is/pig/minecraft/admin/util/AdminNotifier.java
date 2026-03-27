package is.pig.minecraft.admin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

public class AdminNotifier {

    /**
     * Notifies all online admins (OPs) about a moderation event.
     *
     * @param server     The server instance (required for global events with no triggering player)
     * @param sourceName The name of the player or source (e.g., "Dispenser")
     * @param tag        The event tag (e.g., "LAVA", "TNT")
     * @param at         The location of the event (optional)
     * @param worldId    The dimension ID (optional, used for TP links)
     * @param content    The descriptive action content
     */
    public static void notifyAdmins(net.minecraft.server.MinecraftServer server, String sourceName, String tag, BlockPos at, String worldId, Component content) {
        is.pig.minecraft.lib.util.PiggyLog logger = new is.pig.minecraft.lib.util.PiggyLog("piggy-admin", "Admin");
        
        // 1. Log to Console
        if (at != null) {
            logger.info("<{}> [{}] {} @ {} in {}", 
                sourceName, tag, content.getString(), at.toShortString(), worldId != null ? worldId : "unknown");
        } else {
            logger.info("<{}> [{}] {}", sourceName, tag, content.getString());
        }

        if (server == null) return;

        // 2. Build Interactive Elements
        ClickEvent clickEvent = null;
        HoverEvent hoverEvent = null;

        if (at != null && worldId != null) {
            String tpCommand = String.format("/execute in %s run tp @s %d %d %d", worldId, at.getX(), at.getY(), at.getZ());
            String coordsText = String.format("%s: %d, %d, %d", worldId, at.getX(), at.getY(), at.getZ());

            clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand);
            hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(coordsText));
        }

        // 3. Send to OPs
        for (ServerPlayer op : server.getPlayerList().getPlayers()) {
            if (op.hasPermissions(2)) {
                is.pig.minecraft.lib.util.PiggyMessenger.sendAdminMessage(op, sourceName, tag, content, clickEvent, hoverEvent);
            }
        }
    }

    public static void notifyAdmins(ServerPlayer player, String tag, BlockPos at, Component content) {
        if (player == null) return;
        notifyAdmins(player.server, player.getName().getString(), tag, at, 
            player.serverLevel().dimension().location().toString(), content);
    }
}