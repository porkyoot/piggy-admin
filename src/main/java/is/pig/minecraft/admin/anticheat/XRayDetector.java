package is.pig.minecraft.admin.anticheat;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.AntiCheatRule;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.admin.config.PiggyServerConfig;

import java.util.*;

/**
 * Anticheat rule that detects X-Ray usage by analyzing the ratio of rare ores
 * mined compared to common blocks over a sliding time window.
 * 
 * Decoupled from Minecraft internals.
 */
public class XRayDetector implements AntiCheatRule {

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

    private static final Map<UUID, Deque<MineEvent>> playerHistory = new HashMap<>();
    private static final long TIME_WINDOW_MS = 5 * 60 * 1000;

    @Override
    public boolean evaluate(UUID playerUuid, ActionContext context) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();
        if (!config.xrayCheck) return false;

        BlockPos pos = context.pos();
        if (pos.getY() > 64) return false;

        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        String stateId = context.stateId();
        
        boolean isRare = isRareOre(worldState, stateId);
        boolean isCommon = isCommonBlock(worldState, stateId);

        if (!isRare && !isCommon) return false;

        playerHistory.putIfAbsent(playerUuid, new ArrayDeque<>());
        Deque<MineEvent> history = playerHistory.get(playerUuid);

        long now = System.currentTimeMillis();
        history.addLast(new MineEvent(now, isRare, isCommon));
        
        history.removeIf(e -> (now - e.timestamp) > TIME_WINDOW_MS);

        if (history.size() >= config.xrayMinBlocks) {
            return checkRatio(playerUuid, history, pos, config);
        }
        return false;
    }

    private boolean checkRatio(UUID playerUuid, Deque<MineEvent> history, BlockPos pos, PiggyServerConfig config) {
        long rareCount = history.stream().filter(e -> e.isRare).count();
        long commonCount = history.stream().filter(e -> e.isCommon).count();

        long totalTracked = rareCount + commonCount;
        if (totalTracked == 0) return false;

        float ratio = (float) rareCount / (float) totalTracked;

        if (ratio > config.xrayMaxRatio) {
            // Flagged
            if (history.size() > 10) { 
                int toRemove = history.size() / 2;
                for(int i=0; i<toRemove; i++) history.removeFirst();
            }
            return true;
        }
        return false;
    }

    private boolean isRareOre(WorldStateAdapter worldState, String stateId) {
        return worldState.isType(stateId, "diamond_ore") || 
               worldState.isType(stateId, "ancient_debris") || 
               worldState.isType(stateId, "iron_ore") ||
               worldState.isType(stateId, "gold_ore") ||
               worldState.isType(stateId, "emerald_ore");
    }

    private boolean isCommonBlock(WorldStateAdapter worldState, String stateId) {
        return worldState.isType(stateId, "stone") ||
               worldState.isType(stateId, "deepslate") ||
               worldState.isType(stateId, "granite") ||
               worldState.isType(stateId, "diorite") ||
               worldState.isType(stateId, "andesite") ||
               worldState.isType(stateId, "tuff") ||
               worldState.isType(stateId, "netherrack");
    }
}