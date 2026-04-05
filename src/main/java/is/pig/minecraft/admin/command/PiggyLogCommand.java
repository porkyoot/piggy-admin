package is.pig.minecraft.admin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import is.pig.minecraft.admin.storage.HistoryEntry;
import is.pig.minecraft.admin.storage.HistoryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PiggyLogCommand {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private static final SuggestionProvider<CommandSourceStack> HISTORY_PLAYER_SUGGESTIONS = (context, builder) -> {
        for (String name : HistoryManager.getKnownPlayerNames()) {
            builder.suggest(name);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("blame")
                .requires(source -> source.hasPermission(2))
                .executes(PiggyLogCommand::blameBlock));

        dispatcher.register(Commands.literal("logs")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player_name", StringArgumentType.word())
                        .suggests(HISTORY_PLAYER_SUGGESTIONS)
                        .executes(PiggyLogCommand::showLogs)));
    }

    private static int blameBlock(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        if (entity == null) {
            context.getSource().sendFailure(Component.literal("Player only command."));
            return 0;
        }

        HitResult hit = entity.pick(5.0D, 0.0F, false);
        BlockPos pos = (hit.getType() == HitResult.Type.BLOCK) ? ((BlockHitResult) hit).getBlockPos() : entity.blockPosition();
        String worldId = entity.level().dimension().location().toString();

        List<HistoryEntry> entries = HistoryManager.findEntriesNear(worldId, pos, 20.0, 
                "piggy.admin.telemetry.hazardous_placement",
                "piggy.admin.telemetry.chat_moderation",
                "piggy.admin.telemetry.explosion_detonated");

        if (!entries.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Historical incidents within 20 blocks:").withStyle(ChatFormatting.GOLD), false);
            for (HistoryEntry e : entries) {
                formatAndSendEntry(context, e, "Incident");
            }
        } else {
            context.getSource().sendSuccess(() -> Component.literal("No nearby incidents found. Showing global recent:").withStyle(ChatFormatting.YELLOW), false);
            List<HistoryEntry> globals = HistoryManager.getGlobalRecent(5);
            for (HistoryEntry g : globals) {
                formatAndSendEntry(context, g, "Global");
            }
        }

        return 1;
    }

    private static void formatAndSendEntry(CommandContext<CommandSourceStack> context, HistoryEntry entry, String label) {
        String typeLabel = simplifyKey(entry.eventKey());
        String timeStr = entry.timestamp();
        
        Map<String, Object> data = entry.data();
        String playerName = (String) data.getOrDefault("playerName", "System");
        String posStr = String.format("%s, %s, %s", data.getOrDefault("x", "?"), data.getOrDefault("y", "?"), data.getOrDefault("z", "?"));

        MutableComponent component = Component.literal("")
                .append(Component.literal(label + " (" + typeLabel + "): ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(playerName).withStyle(ChatFormatting.RED))
                .append(Component.literal(" @ " + timeStr).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" loc " + posStr).withStyle(ChatFormatting.BLUE))
                .append(Component.literal(" -> " + entry.narrative()).withStyle(ChatFormatting.WHITE));

        // Add additional metadata if available
        if (data.containsKey("sourceEntity")) {
            component.append(Component.literal("\n  Source: ").withStyle(ChatFormatting.YELLOW))
                     .append(Component.literal(String.valueOf(data.get("sourceEntity"))).withStyle(ChatFormatting.GRAY));
        }

        context.getSource().sendSuccess(() -> component, false);
    }

    private static String simplifyKey(String key) {
        if (key.contains("explosion")) return "EXPLOSION";
        if (key.contains("hazard")) return "HAZARD";
        if (key.contains("chat")) return "CHAT";
        return "EVENT";
    }

    private static int showLogs(CommandContext<CommandSourceStack> context) {
        String targetName = StringArgumentType.getString(context, "player_name");
        List<HistoryEntry> entries = HistoryManager.getPlayerHistory(targetName);

        if (entries.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No logs found for " + targetName));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("=== Logs for " + targetName + " ===").withStyle(ChatFormatting.DARK_AQUA), false);

        int start = Math.max(0, entries.size() - 20);
        for (int i = start; i < entries.size(); i++) {
            HistoryEntry e = entries.get(i);
            String timeStr = e.timestamp();
            
            MutableComponent logLine = Component.literal("")
                    .append(Component.literal("[" + timeStr + "] ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("<" + targetName + "> ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("[" + simplifyKey(e.eventKey()) + "] ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(e.narrative()).withStyle(ChatFormatting.WHITE));

            context.getSource().sendSuccess(() -> logLine, false);
        }

        return 1;
    }
}