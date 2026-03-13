package is.pig.minecraft.admin.moderation;

import net.minecraft.server.level.ServerPlayer;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for moderation checks.
 */
public interface ModerationChecker {
    /**
     * Checks if a message should be blocked.
     * @param player The player who sent the message.
     * @param message The message content.
     * @return A CompletableFuture that completes with the ModerationResult indicating the outcome of the check.
     */
    CompletableFuture<ModerationResult> check(ServerPlayer player, String message);

    /**
     * @return The name of this checker.
     */
    String getName();
}
