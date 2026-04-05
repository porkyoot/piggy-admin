package is.pig.minecraft.admin.telemetry;

import is.pig.minecraft.lib.util.perf.PerfMonitor;
import is.pig.minecraft.lib.util.telemetry.StructuredEvent;
import org.slf4j.event.Level;

import java.util.Map;

/**
 * Structured event captured when a chat message is moderated.
 */
public record ChatModerationEvent(
        long timestamp,
        long tick,
        Level level,
        double tps,
        double mspt,
        double cps,
        String pos,
        String playerName,
        String originalMessage,
        String moderationCategory,
        double confidenceScore,
        String actionTaken
) implements StructuredEvent {

    /**
     * Creates a new ChatModerationEvent for server-side auditing.
     * @param playerName         The player involved.
     * @param originalMessage    The message that was moderated.
     * @param moderationCategory The classification (e.g., Toxic, Spam).
     * @param confidenceScore    Gemini's score (0-1).
     * @param actionTaken        The final outcome (BLOCKED, WARNED).
     * @param playerPos          The coordinates of the player.
     * @param currentTick        The server-side game tick.
     */
    public ChatModerationEvent(String playerName, String originalMessage, String moderationCategory, double confidenceScore, String actionTaken, String playerPos, long currentTick) {
        this(
                System.currentTimeMillis(),
                currentTick,
                Level.INFO,
                PerfMonitor.getInstance().getServerTps(),
                50.0, // Default MSPT
                0.0,  // CPS N/A
                playerPos,
                playerName,
                originalMessage,
                moderationCategory,
                confidenceScore,
                actionTaken
        );
    }

    @Override
    public String getEventKey() {
        return "piggy.admin.telemetry.chat_moderation";
    }

    @Override
    public boolean isNotable() {
        return true;
    }

    @Override
    public String getCategoryIcon() {
        return "💬";
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of(
                "playerName", playerName,
                "originalMessage", originalMessage,
                "moderationCategory", moderationCategory,
                "confidenceScore", confidenceScore,
                "actionTaken", actionTaken
        );
    }

    @Override
    public String formatted() {
        return String.format("[%s] [MODERATION] %s: %s | Category: %s | Confidence: %.2f | Action: %s", 
                pos, playerName, originalMessage, moderationCategory, confidenceScore, actionTaken);
    }
}
