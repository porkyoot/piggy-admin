package is.pig.minecraft.admin.moderation;

import java.util.concurrent.CompletableFuture;

public record ModerationResult(boolean blocked, ModerationCategory category, String reason) {
    public static final ModerationResult SAFE = new ModerationResult(false, ModerationCategory.SAFE, "Safe");
    
    public static CompletableFuture<ModerationResult> safeFuture() {
        return CompletableFuture.completedFuture(SAFE);
    }
    
    public static ModerationResult blocked(ModerationCategory category, String reason) {
        return new ModerationResult(true, category, reason);
    }
}
