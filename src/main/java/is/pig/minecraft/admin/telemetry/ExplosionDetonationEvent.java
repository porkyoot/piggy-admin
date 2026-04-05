package is.pig.minecraft.admin.telemetry;

import is.pig.minecraft.lib.util.perf.PerfMonitor;
import is.pig.minecraft.lib.util.telemetry.StructuredEvent;
import org.slf4j.event.Level;
import java.util.Map;

/**
 * Structured event captured when an explosion detonates in the world.
 */
public record ExplosionDetonationEvent(
    long timestamp,
    long tick,
    Level level,
    double tps,
    double mspt,
    double cps,
    String pos,
    String sourceEntity,
    String blockPos,
    float explosionRadius,
    int blocksDestroyedCount
) implements StructuredEvent {

    public ExplosionDetonationEvent(String sourceEntity, String blockPos, float explosionRadius, int blocksDestroyedCount, long currentTick) {
        this(
                System.currentTimeMillis(),
                currentTick,
                Level.INFO,
                PerfMonitor.getInstance().getServerTps(),
                50.0, // Standard server tick target
                0.0,
                blockPos, // Use detonation position as primary 'pos'
                sourceEntity,
                blockPos,
                explosionRadius,
                blocksDestroyedCount
        );
    }

    @Override
    public String getEventKey() {
        return "piggy.admin.telemetry.explosion_detonated";
    }

    @Override
    public boolean isNotable() {
        return true;
    }

    @Override
    public String getCategoryIcon() {
        return "💥";
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of(
            "sourceEntity", sourceEntity,
            "blockPos", blockPos,
            "explosionRadius", explosionRadius,
            "blocksDestroyedCount", blocksDestroyedCount
        );
    }

    @Override
    public String formatted() {
        return String.format("[%s] [EXPLOSION] Source: %s at %s. Radius: %.1f, Blocks: %d", 
                pos, sourceEntity, blockPos, explosionRadius, blocksDestroyedCount);
    }
}
