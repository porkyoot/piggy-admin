package is.pig.minecraft.admin.math;

import java.util.List;

/**
 * Utility class providing mathematical algorithms to deduce suspicious X-Ray behavior.
 * Pure Java, zero Minecraft dependencies.
 */
public final class HeuristicsMathUtil {

    private HeuristicsMathUtil() {
    }

    public static double calculatePotential(Vector3 playerEyePos, List<Vector3> ores) {
        if (ores == null || ores.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (Vector3 pos : ores) {
            Vector3 oreCenter = new Vector3(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
            double distSqr = playerEyePos.distanceToSqr(oreCenter);
            
            if (distSqr > 0.0) {
                total += 1.0 / distSqr;
            }
        }

        return total;
    }

    public static double calculateLookVectorCorrelation(Vector3 playerEyePos, Vector3 playerLookVec, List<Vector3> ores) {
        if (ores == null || ores.isEmpty()) {
            return 0.0;
        }

        Vector3 mostAttractive = null;
        double minSqrDist = Double.MAX_VALUE;

        for (Vector3 pos : ores) {
            Vector3 oreCenter = new Vector3(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
            double distSqr = playerEyePos.distanceToSqr(oreCenter);
            
            if (distSqr < minSqrDist && distSqr > 0.0) {
                minSqrDist = distSqr;
                mostAttractive = pos;
            }
        }

        if (mostAttractive == null) {
            return 0.0;
        }

        Vector3 oreCenter = new Vector3(mostAttractive.x() + 0.5, mostAttractive.y() + 0.5, mostAttractive.z() + 0.5);
        Vector3 dirToOre = oreCenter.subtract(playerEyePos).normalize();
        Vector3 normalizedLook = playerLookVec.normalize();

        return normalizedLook.dot(dirToOre);
    }

    public static double calculateHybridScore(double oldPotential, double newPotential, double lookCorrelation) {
        double deltaP = newPotential - oldPotential;
        
        if (deltaP <= 0.0) {
            return 0.0;
        }
        
        if (lookCorrelation <= 0.0) {
            return 0.0;
        }
        
        return deltaP * lookCorrelation;
    }
}
