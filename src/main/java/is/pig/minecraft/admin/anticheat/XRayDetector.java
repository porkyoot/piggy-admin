package is.pig.minecraft.admin.anticheat;

import is.pig.minecraft.admin.config.PiggyServerConfig;
import is.pig.minecraft.admin.util.AdminNotifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class XRayDetector {

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
    
    // Window size in milliseconds (2 minutes)
    private static final long TIME_WINDOW_MS = 2 * 60 * 1000;

    public static void onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();
        if (!config.xrayCheck) return;

        Block block = state.getBlock();
        boolean isRare = isRareOre(block);
        boolean isCommon = isCommonBlock(block);

        // Optimization: Don't track blocks that are neither rare nor common (like dirt/wood)
        // unless we want strictly "Total Blocks". The prompt specified "vs Stone/Netherrack".
        if (!isRare && !isCommon) return;

        UUID uuid = player.getUUID();
        playerHistory.putIfAbsent(uuid, new ArrayDeque<>());
        Deque<MineEvent> history = playerHistory.get(uuid);

        long now = System.currentTimeMillis();
        
        // 1. Add new event
        history.addLast(new MineEvent(now, isRare, isCommon));

        // 2. Remove old events (older than 2 mins)
        while (!history.isEmpty() && (now - history.peekFirst().timestamp > TIME_WINDOW_MS)) {
            history.removeFirst();
        }

        // 3. Check Ratio
        // We only check if the buffer has enough data to be statistically relevant
        if (history.size() >= config.xrayMinBlocks) {
            checkRatio(player, history, pos, config);
        }
    }

    private static void checkRatio(ServerPlayer player, Deque<MineEvent> history, BlockPos pos, PiggyServerConfig config) {
        int rareCount = 0;
        int commonCount = 0;

        for (MineEvent event : history) {
            if (event.isRare) rareCount++;
            if (event.isCommon) commonCount++;
        }

        int totalTracked = rareCount + commonCount;
        if (totalTracked == 0) return;

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
        }
    }

    private static boolean isRareOre(Block block) {
        return block == Blocks.DIAMOND_ORE || 
               block == Blocks.DEEPSLATE_DIAMOND_ORE || 
               block == Blocks.ANCIENT_DEBRIS;
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