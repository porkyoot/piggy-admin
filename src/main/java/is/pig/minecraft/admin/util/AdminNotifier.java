package is.pig.minecraft.admin.util;

import is.pig.minecraft.admin.PiggyAdmin;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent; // Import this
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminNotifier {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void notifyAdmins(ServerPlayer player, String tag, BlockPos at, Component content) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        
        // 1. Log to Console
        PiggyAdmin.LOGGER.info("[{}] [Admin] <{}> [{}] {} @ {},{},{}", 
            timestamp, player.getName().getString(), tag, content.getString(), at.getX(), at.getY(), at.getZ());

        // 2. Build In-Game Component
        Style whisperStyle = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);

        String tpCommand = String.format("/tp %s %d %d %d", player.getName().getString(), at.getX(), at.getY(), at.getZ());
        String coordsText = String.format("%s: %d, %d, %d", player.serverLevel().dimension().location().toString(), at.getX(), at.getY(), at.getZ());

        // Use MutableComponent
        MutableComponent message = Component.literal("")
                // <PlayerName>
                .append(Component.literal("<" + player.getName().getString() + "> ").withStyle(whisperStyle))
                // [TAG] -> Clickable
                .append(Component.literal("[" + tag + "]")
                        .withStyle(whisperStyle) 
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(coordsText)))
                        ))
                // Content
                .append(Component.literal(" ").withStyle(whisperStyle))
                .append(content.copy().withStyle(whisperStyle));

        // 3. Send to OPs
        for (ServerPlayer op : player.server.getPlayerList().getPlayers()) {
            if (op.hasPermissions(2)) {
                op.sendSystemMessage(message);
            }
        }
    }
}