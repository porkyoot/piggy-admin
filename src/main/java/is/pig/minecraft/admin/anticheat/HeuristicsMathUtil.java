package is.pig.minecraft.admin.anticheat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Utility class providing mathematical algorithms to deduce suspicious X-Ray behavior.
 * Evaluates spatial relationships between player movement, gaze vectors, and cached hidden ores.
 */
public final class HeuristicsMathUtil {

    private HeuristicsMathUtil() {
        // Private constructor for utility class
    }

    /**
     * Calculates the "gravitational potential" of cached ores relative to the player's eye position.
     * Closer ores exert an exponentially higher potential.
     * Formula: Sum of 1.0 / (d * d)
     */
    public static double calculatePotential(Vec3 playerEyePos, List<BlockPos> ores) {
        if (ores == null || ores.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (BlockPos pos : ores) {
            Vec3 oreCenter = Vec3.atCenterOf(pos);
            double distSqr = playerEyePos.distanceToSqr(oreCenter);
            
            // Prevent division by zero if player's eye is exactly inside the block center
            if (distSqr > 0.0) {
                total += 1.0 / distSqr;
            }
        }

        return total;
    }

    /**
     * Calculates the cosine similarity (Dot Product) between the player's line of sight
     * and the vector pointing to the most "attractive" (closest) ore.
     * Returns a value between -1.0 (looking opposite) to 1.0 (looking directly at it).
     */
    public static double calculateLookVectorCorrelation(Vec3 playerEyePos, Vec3 playerLookVec, List<BlockPos> ores) {
        if (ores == null || ores.isEmpty()) {
            return 0.0;
        }

        BlockPos mostAttractive = null;
        double minSqrDist = Double.MAX_VALUE;

        // The ore with the highest 1.0 / (d * d) mathematically corresponds to the lowest distance squared
        for (BlockPos pos : ores) {
            Vec3 oreCenter = Vec3.atCenterOf(pos);
            double distSqr = playerEyePos.distanceToSqr(oreCenter);
            
            if (distSqr < minSqrDist && distSqr > 0.0) {
                minSqrDist = distSqr;
                mostAttractive = pos;
            }
        }

        if (mostAttractive == null) {
            return 0.0;
        }

        Vec3 oreCenter = Vec3.atCenterOf(mostAttractive);
        Vec3 dirToOre = oreCenter.subtract(playerEyePos).normalize();
        Vec3 normalizedLook = playerLookVec.normalize();

        return normalizedLook.dot(dirToOre);
    }

    /**
     * Calculates a hybrid suspicion score based on changes in ore potential and look correlation.
     * Returns a non-zero score only if both the player is moving closer to ores 
     * AND actively looking toward the most attractive ore.
     */
    public static double calculateHybridScore(double oldPotential, double newPotential, double lookCorrelation) {
        double deltaP = newPotential - oldPotential;
        
        // If the player is moving parallel or away from the ores, their behavior is not immediately suspicious
        if (deltaP <= 0.0) {
            return 0.0;
        }
        
        // If the player is not actively looking in the general direction of the core target
        if (lookCorrelation <= 0.0) {
            return 0.0;
        }
        
        // The more directly they stare while moving inward, the higher the score effectively multiplies
        return deltaP * lookCorrelation;
    }
}
