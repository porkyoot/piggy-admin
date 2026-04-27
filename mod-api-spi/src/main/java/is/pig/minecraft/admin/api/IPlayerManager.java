package is.pig.minecraft.admin.api;

import java.util.List;
import java.util.UUID;

/**
 * Pure Java interface for player management and moderation commands.
 */
public interface IPlayerManager {
    List<String> getActionHistory(UUID playerUuid);
    void kickPlayer(UUID playerUuid, String reason);
    void sendWarning(UUID playerUuid, String message);
    void notifyAdmins(UUID playerUuid, String checkName, int x, int y, int z, String message);
}

