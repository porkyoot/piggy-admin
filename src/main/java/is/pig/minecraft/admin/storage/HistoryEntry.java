package is.pig.minecraft.admin.storage;

import java.util.Map;

/**
 * A search result from the JSON history store.
 * Represented as a record to match modern Java features and structured telemetry.
 */
public record HistoryEntry(
    String timestamp,
    String eventKey,
    String narrative,
    Map<String, Object> data
) {
    /**
     * Legacy enum for type-based filtering in commands.
     */
    public enum Type {
        CHAT, SIGN, TNT, EXPLOSION, BLOCK, FIRE, BURN, LAVA
    }

    /**
     * Helper to get playerName from data map safely.
     */
    public String playerName() {
        return (String) data.getOrDefault("playerName", "System");
    }

    /**
     * Helper to get coordinates from data map safely.
     */
    public String posDisplay() {
        Object x = data.get("x");
        Object y = data.get("y");
        Object z = data.get("z");
        if (x != null && y != null && z != null) {
            return String.format("%s, %s, %s", x, y, z );
        }
        return "Unknown";
    }
}