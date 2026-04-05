package is.pig.minecraft.admin.util;

import is.pig.minecraft.admin.PiggyAdmin;
import is.pig.minecraft.lib.util.telemetry.StructuredEvent;
import is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminNotifier {

    /**
     * Registers the AdminNotifier as a listener to the global event dispatcher.
     */
    public static void register() {
        is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance().registerListener(view -> {
            if (view.parent().isNotable() || view.parent().isFailure()) {
                broadcastAdminEvent(view.parent());
            }
        });
    }

    /**
     * Broadcasts a structured event to all administrators with interactive elements.
     * Includes clickable teleport links for coordinates and [Kick]/[Ban] buttons.
     */
    public static void broadcastAdminEvent(StructuredEvent event) {
        String narrative = PiggyTelemetryFormatter.formatNarrative(event);
        if (narrative == null || narrative.isEmpty()) return;

        MinecraftServer server = PiggyAdmin.getServer();
        if (server == null) return;

        MutableComponent message = Component.empty();
        
        // Use different prefix/style for failures vs notable threats
        if (event.isFailure()) {
            message.append(Component.literal("[System Alert] ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        }

        // Regex for coordinates: X, Y, Z (handles integers, decimals, and optional parentheses)
        Pattern coordPattern = Pattern.compile("(\\(?-?\\d+\\.?\\d*,\\s*-?\\d+\\.?\\d*,\\s*-?\\d+\\.?\\d*\\)?)");
        Matcher matcher = coordPattern.matcher(narrative);
        
        int lastEnd = 0;
        while (matcher.find()) {
            message.append(Component.literal(narrative.substring(lastEnd, matcher.start())));
            String rawCoords = matcher.group(1);
            
            // Clean up coordinates for the command: remove parentheses, normalize commas to spaces, and trim
            String commandCoords = rawCoords.replaceAll("[()]", "").replace(",", " ").replaceAll("\\s+", " ").trim();
            String tpCommand = "/tp @s " + commandCoords;
            
            message.append(Component.literal(rawCoords)
                .withStyle(s -> s.withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
                )
            );
            lastEnd = matcher.end();
        }
        message.append(Component.literal(narrative.substring(lastEnd)));

        // Append [Kick] and [Ban] buttons ONLY for notable (human) threats, not system failures
        if (event.isNotable()) {
            String playerName = (String) event.getEventData().get("playerName");
            if (playerName != null) {
                message.append(" ");
                message.append(Component.literal("[Kick]")
                    .withStyle(s -> s.withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/kick " + playerName + " Griefing/Moderation"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Suggest Kick for " + playerName)))
                    )
                );
                message.append(" ");
                message.append(Component.literal("[Ban]")
                    .withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ban " + playerName + " Griefing/Moderation"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Suggest Ban for " + playerName)))
                    )
                );
            }
        }

        // Broadcast to all players with permission level 2 (Admins)
        for (ServerPlayer op : server.getPlayerList().getPlayers()) {
            if (op.hasPermissions(2)) {
                op.sendSystemMessage(message);
            }
        }
        
        // Also log the final narrative to console for verification
        PiggyAdmin.LOGGER.info("[Admin Notification] {}", narrative);
    }

    /**
     * Legacy notification method (retained for backward compatibility if needed).
     */
    public static void notifyAdmins(MinecraftServer server, String sourceName, String tag, BlockPos at, String worldId, Component content) {
        if (server == null) return;
        
        ClickEvent clickEvent = null;
        HoverEvent hoverEvent = null;

        if (at != null && worldId != null) {
            String tpCommand = String.format("/execute in %s run tp @s %d %d %d", worldId, at.getX(), at.getY(), at.getZ());
            String coordsText = String.format("%s: %d, %d, %d", worldId, at.getX(), at.getY(), at.getZ());
            clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand);
            hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(coordsText));
        }

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