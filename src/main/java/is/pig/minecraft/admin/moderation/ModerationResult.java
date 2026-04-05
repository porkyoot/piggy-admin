package is.pig.minecraft.admin.moderation;

import java.util.concurrent.CompletableFuture;

/**
 * Result of a moderation check.
 * @param blocked         Whether the message should be blocked.
 * @param category        The classification of the message.
 * @param reason          A human-readable reason for the outcome.
 * @param confidenceScore The confidence score of the classification (0.0 to 1.0).
 */
public record ModerationResult(boolean blocked, ModerationCategory category, String reason, double confidenceScore) {
    public static final ModerationResult SAFE = new ModerationResult(false, ModerationCategory.SAFE, "Safe", 1.0);
    
    public static CompletableFuture<ModerationResult> safeFuture() {
        return CompletableFuture.completedFuture(SAFE);
    }
    
    public static ModerationResult blocked(ModerationCategory category, String reason, double confidenceScore) {
        return new ModerationResult(true, category, reason, confidenceScore);
    }
}
