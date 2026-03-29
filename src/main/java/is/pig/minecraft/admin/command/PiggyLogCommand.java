package is.pig.minecraft.admin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider; // Import
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

import java.util.List;
import java.util.Map;

public class PiggyLogCommand {

    // NEW: Suggestion Provider that fetches names from HistoryManager
    private static final SuggestionProvider<CommandSourceStack> HISTORY_PLAYER_SUGGESTIONS = (context, builder) -> {
        for (String name : HistoryManager.getKnownPlayerNames()) {
            // Suggest every name found in the history
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
                        .suggests(HISTORY_PLAYER_SUGGESTIONS) // Apply suggestions here
                        .executes(PiggyLogCommand::showLogs)));
    }

    private static int blameBlock(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        if (entity == null) {
            context.getSource().sendFailure(Component.literal("Player only command."));
            return 0;
        }

        HitResult hit = entity.pick(5.0D, 0.0F, false);
        BlockPos pos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            pos = ((BlockHitResult) hit).getBlockPos();
        } else {
            pos = entity.blockPosition();
        }
        
        String worldId = entity.level().dimension().location().toString();

        // 1. Check for nearby historical incidents (20 block radius)
        List<HistoryEntry> entries = HistoryManager.findEntriesNear(worldId, pos, 20.0, 
                HistoryEntry.Type.SIGN, HistoryEntry.Type.TNT, HistoryEntry.Type.FIRE, 
                HistoryEntry.Type.BURN, HistoryEntry.Type.BLOCK, HistoryEntry.Type.LAVA);

        if (!entries.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Historical incidents within 20 blocks:").withStyle(ChatFormatting.GOLD), false);
            for (HistoryEntry e : entries) {
                formatAndSendEntry(context, e, "Incident");
            }
        } else {
            context.getSource().sendSuccess(() -> Component.literal("No nearby incidents found within 20 blocks. Showing global recent:").withStyle(ChatFormatting.YELLOW), false);
        }

        // 2. Also check for explosions that might have destroyed this area
        List<HistoryEntry> explosions = HistoryManager.findExplosionsAffecting(worldId, pos);
        if (!explosions.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Explosions affecting this area:").withStyle(ChatFormatting.GOLD), false);
            for (HistoryEntry ex : explosions) {
                formatAndSendEntry(context, ex, "Explosion");
            }
        }

        // 3. Always show top 3 global recent as a "Top-Off" to ensure something is always visible
        List<HistoryEntry> globals = HistoryManager.getGlobalRecent(3);
        boolean showedGlobalHeader = false;
        for (HistoryEntry g : globals) {
            // Don't duplicate if it was already shown in nearby or explosions
            if (entries.contains(g) || explosions.contains(g)) continue;
            
            if (!showedGlobalHeader) {
                context.getSource().sendSuccess(() -> Component.literal("--- Global Recent ---").withStyle(ChatFormatting.DARK_GRAY), false);
                showedGlobalHeader = true;
            }
            formatAndSendEntry(context, g, "Global");
        }
        
        return 1;
    }

    private static void formatAndSendEntry(CommandContext<CommandSourceStack> context, HistoryEntry entry, String label) {
        String typeLabel = (entry.type == HistoryEntry.Type.SIGN) ? "Sign" : 
                          (entry.type == HistoryEntry.Type.TNT) ? "TNT/Responsibility" : 
                          (entry.type == HistoryEntry.Type.FIRE) ? "Ignition" :
                          (entry.type == HistoryEntry.Type.LAVA) ? "Lava" :
                          (entry.type == HistoryEntry.Type.BURN) ? "Fire Damage" : "Explosion";
        
        Map<String, String> meta = entry.getMetadata();
        String playerDisplay = meta.getOrDefault("forensic_player", entry.playerName != null ? entry.playerName : "Unknown");
        String posDisplay = meta.getOrDefault("forensic_block", String.format("%d, %d, %d", entry.x, entry.y, entry.z));

        MutableComponent component = Component.literal("")
                .append(Component.literal(label + " (" + typeLabel + "): ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(playerDisplay).withStyle(ChatFormatting.RED))
                .append(Component.literal(" @ " + entry.timestamp).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" loc " + posDisplay).withStyle(ChatFormatting.BLUE))
                .append(Component.literal(" -> " + (entry.content != null ? entry.content : "")).withStyle(ChatFormatting.WHITE));

        // Add metadata if available
        if (meta.containsKey("source")) {
            component.append(Component.literal("\n  Source: ").withStyle(ChatFormatting.YELLOW))
                     .append(Component.literal(meta.get("source")).withStyle(ChatFormatting.GRAY));
        }
        if (meta.containsKey("nearby_players")) {
            component.append(Component.literal("\n  Nearby: ").withStyle(ChatFormatting.AQUA))
                     .append(Component.literal(meta.get("nearby_players")).withStyle(ChatFormatting.GRAY));
        }
        if (meta.containsKey("victims")) {
            component.append(Component.literal("\n  Victims: ").withStyle(ChatFormatting.DARK_RED))
                     .append(Component.literal(meta.get("victims")).withStyle(ChatFormatting.WHITE));
        }

        context.getSource().sendSuccess(() -> component, false);
    }

    private static int showLogs(CommandContext<CommandSourceStack> context) {
        String targetName = StringArgumentType.getString(context, "player_name");
        List<HistoryEntry> entries = HistoryManager.getPlayerHistory(targetName);

        if (entries.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No logs found for " + targetName));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("=== Logs for " + targetName + " ===").withStyle(ChatFormatting.DARK_AQUA), false);

        int start = Math.max(0, entries.size() - 20); // Show more logs
        for (int i = start; i < entries.size(); i++) {
            HistoryEntry e = entries.get(i);
            String actionTag = e.type.name();
            
            Map<String, String> meta = e.getMetadata();
            String playerDisplay = meta.getOrDefault("forensic_player", e.playerName != null ? e.playerName : "Unknown");

            MutableComponent logLine = Component.literal("")
                    .append(Component.literal("[" + e.timestamp + "] ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("<" + playerDisplay + "> ").withStyle(ChatFormatting.GRAY));

            MutableComponent tagComponent = Component.literal("[" + actionTag + "]").withStyle(ChatFormatting.YELLOW);
            
            if ((e.type == HistoryEntry.Type.SIGN || e.type == HistoryEntry.Type.TNT || 
                 e.type == HistoryEntry.Type.EXPLOSION || e.type == HistoryEntry.Type.FIRE || 
                 e.type == HistoryEntry.Type.BURN || e.type == HistoryEntry.Type.LAVA) && e.worldId != null) {
                String tpCmd = String.format("/execute in %s run tp @s %d %d %d", e.worldId, e.x, e.y, e.z);
                String hoverText = String.format("%s: %d, %d, %d\nClick to TP", e.worldId, e.x, e.y, e.z);
                
                tagComponent = tagComponent.withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCmd))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
                        .withUnderlined(true));
            }

            logLine = logLine.append(tagComponent)
                             .append(Component.literal(" " + (e.content != null ? e.content : "")).withStyle(ChatFormatting.WHITE));

            // Add rich metadata summary to log line
            if (meta.containsKey("source")) {
                logLine.append(Component.literal(" [Source: " + meta.get("source") + "]").withStyle(ChatFormatting.YELLOW));
            }
            if (meta.containsKey("victims")) {
                logLine.append(Component.literal(" [Victims: " + meta.get("victims") + "]").withStyle(ChatFormatting.RED));
            }

            final Component finalLogLine = logLine;
            context.getSource().sendSuccess(() -> finalLogLine, false);
        }

        return 1;
    }
}