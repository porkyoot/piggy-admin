package is.pig.minecraft.admin.anticheat;

import is.pig.minecraft.admin.api.IPlayerManager;
import is.pig.minecraft.admin.api.IWorldAnalyzer;
import is.pig.minecraft.admin.math.HeuristicsMathUtil;
import is.pig.minecraft.admin.math.Vector3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player mining heuristics over time to identify suspicious "gradient" movements 
 * toward hidden high-value ores.
 * Pure Java, zero Minecraft dependencies.
 */
public class HybridXRayDetector {

    private static class PlayerTrackingData {
        double lastKnownPotential = 0.0;
        final Deque<Double> rollingWindow = new ArrayDeque<>(20);
    }

    private final Map<UUID, PlayerTrackingData> trackingMap = new ConcurrentHashMap<>();

    public boolean evaluate(UUID playerUuid, Vector3 eyePos, Vector3 lookVec, 
                            int blockX, int blockY, int blockZ, String blockId, 
                            boolean isNether, boolean xrayHybridCheck, double xrayHybridThreshold, 
                            IWorldAnalyzer worldAnalyzer, IPlayerManager playerManager) {
        
        if (!blockId.equals("minecraft:stone") && !blockId.equals("minecraft:deepslate") && 
            !blockId.equals("minecraft:netherrack") && !blockId.equals("minecraft:tuff")) {
            return false;
        }

        if (!isNether && blockY >= 40) {
            return false; 
        }

        // Check for exposed faces (natural cave discovery)
        int[][] offsets = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
        };
        boolean hasExposedFace = false;
        for (int[] offset : offsets) {
            String neighborId = worldAnalyzer.getBlockId(blockX + offset[0], blockY + offset[1], blockZ + offset[2]);
            if (neighborId.equals("minecraft:air") || neighborId.equals("minecraft:water") || 
                neighborId.equals("minecraft:cave_air") || neighborId.equals("minecraft:void_air")) {
                hasExposedFace = true;
                break;
            }
        }
        if (hasExposedFace) {
            return false;
        }

        if (!xrayHybridCheck) {
            return false;
        }

        List<Vector3> ores = OreCacheManager.INSTANCE.getOresInRadius(blockX, blockY, blockZ, 15);
        if (ores.isEmpty()) {
            return false;
        }

        double newPotential = HeuristicsMathUtil.calculatePotential(eyePos, ores);
        double lookCorrelation = HeuristicsMathUtil.calculateLookVectorCorrelation(eyePos, lookVec, ores);

        PlayerTrackingData data = trackingMap.computeIfAbsent(playerUuid, k -> new PlayerTrackingData());
        double oldPotential = data.lastKnownPotential;

        double hybridScore = HeuristicsMathUtil.calculateHybridScore(oldPotential, newPotential, lookCorrelation);

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

            if (average > xrayHybridThreshold) {
                playerManager.notifyAdmins(
                        playerUuid, 
                        "XRAY-HYBRID", 
                        blockX, blockY, blockZ,
                        String.format("Suspicious hybrid gradient score: %.4f", average)
                );
                
                data.rollingWindow.clear();
                return true;
            }
        }

        return false;
    }
}
