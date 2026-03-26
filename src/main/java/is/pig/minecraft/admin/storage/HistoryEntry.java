package is.pig.minecraft.admin.storage;

import is.pig.minecraft.admin.moderation.ModerationCategory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HistoryEntry {
    public enum Type {
        CHAT, SIGN, TNT, EXPLOSION, BLOCK
    }

    public String timestamp;
    public String playerName;
    public String playerUuid;
    public Type type;
    public String content;
    public ModerationCategory category; // Category for BLOCK result
    
    // Metadata for rich context (e.g., nearby players, victims, radius)
    public Map<String, String> metadata = new HashMap<>();

    // Location data
    public String worldId;
    public int x;
    public int y;
    public int z;

    public HistoryEntry(String timestamp, String playerName, UUID playerUuid, Type type, String content) {
        this.timestamp = timestamp;
        this.playerName = playerName;
        this.playerUuid = playerUuid != null ? playerUuid.toString() : null;
        this.type = type;
        this.content = content;
    }

    public HistoryEntry setCategory(ModerationCategory category) {
        this.category = category;
        return this;
    }

    public Map<String, String> getMetadata() {
        if (metadata == null) metadata = new HashMap<>();
        return metadata;
    }

    public HistoryEntry putMetadata(String key, String value) {
        getMetadata().put(key, value);
        return this;
    }

    public void setLocation(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}