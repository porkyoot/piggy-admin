package is.pig.minecraft.admin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

public class AdminNotifier {

    public static void notifyAdmins(ServerPlayer player, String tag, BlockPos at, Component content) {
        // 1. Log to Console
        is.pig.minecraft.lib.util.PiggyLog logger = new is.pig.minecraft.lib.util.PiggyLog("piggy-admin", "Admin");
        logger.info("<{}> [{}] {} @ {},{},{}", 
            player.getName().getString(), tag, content.getString(), at.getX(), at.getY(), at.getZ());

        // 2. Build Interactive Elements
        String dimension = player.serverLevel().dimension().location().toString();
        String tpCommand = String.format("/execute in %s run tp @s %d %d %d", dimension, at.getX(), at.getY(), at.getZ());
        String coordsText = String.format("%s: %d, %d, %d", dimension, at.getX(), at.getY(), at.getZ());

        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand);
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(coordsText));

        // 3. Send to OPs
        for (ServerPlayer op : player.server.getPlayerList().getPlayers()) {
            if (op.hasPermissions(2)) {
                is.pig.minecraft.lib.util.PiggyMessenger.sendAdminMessage(op, player.getName().getString(), tag, content, clickEvent, hoverEvent);
            }
        }
    }
}