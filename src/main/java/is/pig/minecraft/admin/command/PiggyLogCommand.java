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
                .executes(PiggyLogCommand::blameSign));

        dispatcher.register(Commands.literal("logs")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player_name", StringArgumentType.word())
                        .suggests(HISTORY_PLAYER_SUGGESTIONS) // Apply suggestions here
                        .executes(PiggyLogCommand::showLogs)));
    }

    private static int blameSign(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        if (entity == null) {
            context.getSource().sendFailure(Component.literal("Player only command."));
            return 0;
        }

        HitResult hit = entity.pick(5.0D, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            context.getSource().sendFailure(Component.literal("Look at a block."));
            return 0;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        String worldId = entity.level().dimension().location().toString();

        HistoryEntry entry = HistoryManager.getSignInfo(worldId, pos);

        if (entry == null) {
            context.getSource().sendFailure(Component.literal("No history for this block."));
        } else {
            context.getSource().sendSuccess(() -> Component.literal("")
                    .append(Component.literal("Sign Blame: ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(entry.playerName).withStyle(ChatFormatting.RED))
                    .append(Component.literal(" @ " + entry.timestamp).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" -> " + entry.content).withStyle(ChatFormatting.WHITE)), false);
        }
        return 1;
    }

    private static int showLogs(CommandContext<CommandSourceStack> context) {
        String targetName = StringArgumentType.getString(context, "player_name");
        List<HistoryEntry> entries = HistoryManager.getPlayerHistory(targetName);

        if (entries.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No logs found for " + targetName));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("=== Logs for " + targetName + " ===").withStyle(ChatFormatting.DARK_AQUA), false);

        int start = Math.max(0, entries.size() - 10);
        for (int i = start; i < entries.size(); i++) {
            HistoryEntry e = entries.get(i);
            String actionTag = (e.type == HistoryEntry.Type.CHAT) ? "CHAT" : "SIGN";
            
            MutableComponent logLine = Component.literal("")
                    .append(Component.literal("[" + e.timestamp + "] ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("<" + e.playerName + "> ").withStyle(ChatFormatting.GRAY));

            MutableComponent tagComponent = Component.literal("[" + actionTag + "]").withStyle(ChatFormatting.YELLOW);
            
            if (e.type == HistoryEntry.Type.SIGN && e.worldId != null) {
                String tpCmd = String.format("/tp %s %d %d %d", targetName, e.x, e.y, e.z);
                String hoverText = String.format("%s: %d, %d, %d", e.worldId, e.x, e.y, e.z);
                
                tagComponent = tagComponent.withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCmd))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
                        .withUnderlined(true));
            }

            logLine = logLine.append(tagComponent)
                             .append(Component.literal(" " + e.content).withStyle(ChatFormatting.WHITE));

            final Component finalLogLine = logLine;
            context.getSource().sendSuccess(() -> finalLogLine, false);
        }

        return 1;
    }
}