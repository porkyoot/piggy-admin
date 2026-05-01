package is.pig.minecraft.admin.anticheat;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.AntiCheatRule;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.admin.config.PiggyServerConfig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player mining heuristics over time to identify suspicious "gradient" movements 
 * toward hidden high-value ores.
 * 
 * Decoupled from Minecraft internals.
 */
public class HybridXRayDetector implements AntiCheatRule {

    private static class PlayerTrackingData {
        double lastKnownPotential = 0.0;
        final Deque<Double> rollingWindow = new ArrayDeque<>(20);
    }

    private final Map<UUID, PlayerTrackingData> trackingMap = new ConcurrentHashMap<>();

    @Override
    public boolean evaluate(UUID playerUuid, ActionContext context) {
        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        String stateId = context.stateId();

        // Check if the broken block is a common subterranean rock type
        if (!worldState.isType(stateId, "stone") && !worldState.isType(stateId, "deepslate") && 
            !worldState.isType(stateId, "netherrack") && !worldState.isType(stateId, "tuff")) {
            return false;
        }

        String worldId = context.worldId();
        int y = context.pos().getY();

        // Validate depth thresholds (simplified for decoupled logic)
        if (!worldId.contains("nether") && y >= 40) {
            return false; 
        }

        // Ignore if the mined block is adjacent to an air or water face
        if (worldState.isExposed(worldId, context.pos())) {
            return false;
        }

        PiggyServerConfig config = PiggyServerConfig.getInstance();
        if (!config.xrayHybridCheck) {
            return false;
        }

        // Scan for recently cached ores within a 15-block localized scope
        List<BlockPos> ores = worldState.getOresInRadius(worldId, context.pos(), 15);
        if (ores.isEmpty()) {
            return false;
        }

        Vec3 eyePos = context.eyePos();
        Vec3 lookVec = context.lookVec();

        double newPotential = HeuristicsMathUtil.calculatePotential(eyePos, ores);
        double lookCorrelation = HeuristicsMathUtil.calculateLookVectorCorrelation(eyePos, lookVec, ores);

        // Fetch running history
        PlayerTrackingData data = trackingMap.computeIfAbsent(playerUuid, k -> new PlayerTrackingData());
        double oldPotential = data.lastKnownPotential;

        double hybridScore = HeuristicsMathUtil.calculateHybridScore(oldPotential, newPotential, lookCorrelation);

        // Update tracking states
        data.lastKnownPotential = newPotential;
        
        if (data.rollingWindow.size() >= 20) {
            data.rollingWindow.pollFirst();
        }
        data.rollingWindow.addLast(hybridScore);

        if (data.rollingWindow.size() == 20) {
            double sum = 0.0;
            for (double score : data.rollingWindow) {
                sum += score;
            }
            double average = sum / 20.0;

            if (average > config.xrayHybridThreshold) {
                // Notifications will be handled by the core orchestrator or a decoupled notifier
                // For now, we return true to flag the violation
                data.rollingWindow.clear();
                return true;
            }
        }

        return false;
    }
}
