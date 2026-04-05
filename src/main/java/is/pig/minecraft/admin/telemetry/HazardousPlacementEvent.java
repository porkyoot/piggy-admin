package is.pig.minecraft.admin.telemetry;

import is.pig.minecraft.lib.util.perf.PerfMonitor;
import is.pig.minecraft.lib.util.telemetry.StructuredEvent;
import org.slf4j.event.Level;
import java.util.Map;

/**
 * Unified structured event for high-risk item/block placements.
 */
public record HazardousPlacementEvent(
    long timestamp,
    long tick,
    Level level,
    double tps,
    double mspt,
    double cps,
    String pos,
    String playerName,
    String targetName,
    String blockPos,
    String dimension,
    PlacementType type
) implements StructuredEvent {

    public enum PlacementType {
        ARSON("🔥"),
        HAZARD("🧪"),
        THREAT("💣");

        private final String icon;
        PlacementType(String icon) { this.icon = icon; }
        public String getIcon() { return icon; }
    }

    public HazardousPlacementEvent(String playerName, String targetName, String blockPos, String dimension, String playerPos, long currentTick, PlacementType type) {
        this(
                System.currentTimeMillis(),
                currentTick,
                Level.WARN,
                PerfMonitor.getInstance().getServerTps(),
                50.0,
                0.0,
                playerPos,
                playerName,
                targetName,
                blockPos,
                dimension,
                type
        );
    }

    @Override
    public String getEventKey() {
        return "piggy.admin.telemetry.hazardous_placement";
    }

    @Override
    public boolean isNotable() {
        return true;
    }

    @Override
    public String getCategoryIcon() {
        return type.getIcon();
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of(
            "playerName", playerName,
            "targetName", targetName,
            "blockPos", blockPos,
            "dimension", dimension,
            "type", type.name()
        );
    }

    @Override
    public String formatted() {
        return String.format("[%s] [%s] %s placed %s at %s", 
                pos, type.name(), playerName, targetName, blockPos);
    }
}
