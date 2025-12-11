package is.pig.minecraft.admin.storage;

import java.util.UUID;

public class HistoryEntry {
    public enum Type {
        CHAT, SIGN
    }

    public String timestamp;
    public String playerName;
    public String playerUuid;
    public Type type;
    public String content;
    
    // Location data (only relevant for SIGN type)
    public String worldId;
    public int x;
    public int y;
    public int z;

    public HistoryEntry(String timestamp, String playerName, UUID playerUuid, Type type, String content) {
        this.timestamp = timestamp;
        this.playerName = playerName;
        this.playerUuid = playerUuid.toString();
        this.type = type;
        this.content = content;
    }

    public void setLocation(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}