package is.pig.minecraft.admin.anticheat;

import is.pig.minecraft.admin.config.PiggyServerConfig;
import is.pig.minecraft.admin.util.AdminNotifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public class XRayDetector implements IAntiCheatRule {

    // A simple record to store what was mined and when
    private static class MineEvent {
        long timestamp;
        boolean isRare;
        boolean isCommon;

        MineEvent(long timestamp, boolean isRare, boolean isCommon) {
            this.timestamp = timestamp;
            this.isRare = isRare;
            this.isCommon = isCommon;
        }
    }

    // Stores history per player
    private static final Map<UUID, Deque<MineEvent>> playerHistory = new HashMap<>();
    
    // Window size in milliseconds (5 minutes)
    private static final long TIME_WINDOW_MS = 5 * 60 * 1000;

    @Override
    public boolean evaluate(ServerPlayer player, ActionContext context) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();
        if (!config.xrayCheck) return false;

        BlockPos pos = context.pos();
        // Ignore blocks mined above y=64
        if (pos.getY() > 64) return false;

        Block block = context.state().getBlock();
        boolean isRare = isRareOre(block);
        boolean isCommon = isCommonBlock(block);

        // Optimization: Don't track blocks that are neither rare nor common (like dirt/wood)
        // unless we want strictly "Total Blocks". The prompt specified "vs Stone/Netherrack".
        if (!isRare && !isCommon) return false;

        UUID uuid = player.getUUID();
        playerHistory.putIfAbsent(uuid, new ArrayDeque<>());
        Deque<MineEvent> history = playerHistory.get(uuid);

        long now = System.currentTimeMillis();
        
        // 1. Add new event
        history.addLast(new MineEvent(now, isRare, isCommon));

        // 2. Remove old events (older than 2 mins) using Streams API
        java.util.List<MineEvent> validEvents = history.stream()
                .filter(e -> (now - e.timestamp) <= TIME_WINDOW_MS)
                .toList();
        
        history.clear();
        history.addAll(validEvents);

        // 3. Check Ratio
        // We only check if the buffer has enough data to be statistically relevant
        if (history.size() >= config.xrayMinBlocks) {
            return checkRatio(player, history, pos, config);
        }
        return false;
    }

    private boolean checkRatio(ServerPlayer player, Deque<MineEvent> history, BlockPos pos, PiggyServerConfig config) {
        long rareCount = history.stream().filter(e -> e.isRare).count();
        long commonCount = history.stream().filter(e -> e.isCommon).count();

        long totalTracked = rareCount + commonCount;
        if (totalTracked == 0) return false;

        // Ratio: Rare / (Rare + Common)
        float ratio = (float) rareCount / (float) totalTracked;

        if (ratio > config.xrayMaxRatio) {
            // cooldown check could go here to prevent spam, 
            // but AdminNotifier is distinct enough.
            
            // Format percentage
            String percentage = String.format("%.1f%%", ratio * 100);
            
            // Alert Message: "Ratio: 45.0% (8 Rare / 12 Common)"
            Component content = Component.literal("Ratio: ")
                    .append(Component.literal(percentage).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                    .append(Component.literal(String.format(" (%d Rare / %d Common)", rareCount, commonCount))
                            .withStyle(ChatFormatting.GRAY));

            AdminNotifier.notifyAdmins(player, "XRAY", pos, content);
            
            // Clear history partially to prevent spamming the same alert every block break?
            // Or just let it spam (as they are still xraying). 
            // Clearing 50% of history is a simple "cooldown".
            if (history.size() > 10) { 
                // remove oldest half
                int toRemove = history.size() / 2;
                for(int i=0; i<toRemove; i++) history.removeFirst();
            }
            return true;
        }
        return false;
    }

    private static boolean isRareOre(Block block) {
        return block == Blocks.DIAMOND_ORE || 
               block == Blocks.DEEPSLATE_DIAMOND_ORE || 
               block == Blocks.ANCIENT_DEBRIS ||
               block == Blocks.IRON_ORE ||
               block == Blocks.DEEPSLATE_IRON_ORE ||
               block == Blocks.GOLD_ORE ||
               block == Blocks.DEEPSLATE_GOLD_ORE ||
               block == Blocks.EMERALD_ORE ||
               block == Blocks.DEEPSLATE_EMERALD_ORE;
    }

    private static boolean isCommonBlock(Block block) {
        return block == Blocks.STONE ||
               block == Blocks.DEEPSLATE ||
               block == Blocks.GRANITE ||
               block == Blocks.DIORITE ||
               block == Blocks.ANDESITE ||
               block == Blocks.TUFF ||
               block == Blocks.NETHERRACK;
    }
}