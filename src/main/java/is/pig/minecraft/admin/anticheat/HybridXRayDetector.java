package is.pig.minecraft.admin.anticheat;

import is.pig.minecraft.admin.util.AdminNotifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player mining heuristics over time to identify suspicious "gradient" movements 
 * toward hidden high-value ores.
 */
public class HybridXRayDetector implements IAntiCheatRule {

    private static class PlayerTrackingData {
        double lastKnownPotential = 0.0;
        final Deque<Double> rollingWindow = new ArrayDeque<>(20);
    }

    private final Map<UUID, PlayerTrackingData> trackingMap = new ConcurrentHashMap<>();

    @Override
    public boolean evaluate(ServerPlayer player, ActionContext context) {
        BlockState state = context.state();

        // Check if the broken block is a common subterranean rock type
        if (!state.is(Blocks.STONE) && !state.is(Blocks.DEEPSLATE) && 
            !state.is(Blocks.NETHERRACK) && !state.is(Blocks.TUFF)) {
            return false;
        }

        Level level = context.level();
        boolean isNether = level.dimension() == Level.NETHER;
        int y = context.pos().getY();

        // Validate depth thresholds
        if (!isNether && y >= 16) {
            return false; // Not deep enough in the Overworld
        }
        if (isNether && (y < 8 || y > 119)) {
            return false; // Not in the primary dangerous strata in the Nether
        }

        // Ignore if the block is exposed to light (e.g., natural cave or player-placed torches) 
        // because legitimate players follow cave systems naturally.
        if (level.getBrightness(LightLayer.BLOCK, context.pos()) > 0) {
            return false;
        }

        // Constraints passed, perform heuristic calculations
        ServerLevel serverLevel = (ServerLevel) level;
        
        // Scan for recently cached ores within a 15-block localized scope
        List<BlockPos> ores = OreCacheManager.INSTANCE.getOresInRadius(serverLevel, context.pos(), 15);
        if (ores.isEmpty()) {
            return false;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);

        double newPotential = HeuristicsMathUtil.calculatePotential(eyePos, ores);
        double lookCorrelation = HeuristicsMathUtil.calculateLookVectorCorrelation(eyePos, lookVec, ores);

        // Fetch running history or bootstrap a fresh one locally
        PlayerTrackingData data = trackingMap.computeIfAbsent(player.getUUID(), k -> new PlayerTrackingData());
        double oldPotential = data.lastKnownPotential;

        double hybridScore = HeuristicsMathUtil.calculateHybridScore(oldPotential, newPotential, lookCorrelation);

        // Update tracking states dynamically
        data.lastKnownPotential = newPotential;
        
        if (data.rollingWindow.size() >= 20) {
            data.rollingWindow.pollFirst();
        }
        data.rollingWindow.addLast(hybridScore);

        // Evaluate only when the 20-block window is fully saturated
        if (data.rollingWindow.size() == 20) {
            double sum = 0.0;
            for (double score : data.rollingWindow) {
                sum += score;
            }
            double average = sum / 20.0;

            // Target threshold trigger
            if (average > 0.05) {
                AdminNotifier.notifyAdmins(
                        player, 
                        "XRAY-HYBRID", 
                        context.pos(), 
                        Component.literal(String.format("Suspicious hybrid gradient score: %.4f", average))
                );
                
                // Clear the window strictly to enforce cooldown and prevent alert spam
                data.rollingWindow.clear();
                return true;
            }
        }

        return false;
    }
}
