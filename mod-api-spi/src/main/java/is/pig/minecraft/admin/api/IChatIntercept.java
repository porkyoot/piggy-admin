package is.pig.minecraft.admin.api;

import java.util.UUID;

/**
 * Pure Java interface for chat message representation and interception.
 */
public interface IChatIntercept {
    String getMessage();
    String getSenderName();
    UUID getSenderUuid();
    void cancel();
    boolean isCancelled();
}

