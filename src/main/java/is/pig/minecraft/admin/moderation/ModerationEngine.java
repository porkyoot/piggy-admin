package is.pig.minecraft.admin.moderation;

import is.pig.minecraft.admin.storage.HistoryManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.ChatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModerationEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModerationEngine");
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static ModerationEngine INSTANCE;
    private final List<ModerationChecker> checkers = new ArrayList<>();

    // Use identity tracking to prevent infinite loops even if signatures are missing/clashing
    private final Set<PlayerChatMessage> moderatedMessages = Collections.newSetFromMap(Collections.synchronizedMap(new java.util.IdentityHashMap<>()));

    public ModerationEngine() {
        checkers.add(new RegexModerationChecker());
        checkers.add(new GeminiModerationChecker());
    }

    public static ModerationEngine getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModerationEngine();
        }
        return INSTANCE;
    }

    public void reload() {
        for (ModerationChecker checker : checkers) {
            if (checker instanceof RegexModerationChecker regexChecker) {
                regexChecker.reload();
            }
        }
        moderatedMessages.clear();
    }

    /**
     * Checks a message and takes action if needed.
     */
    public CompletableFuture<Boolean> processMessage(ServerPlayer player, PlayerChatMessage message, ChatType.Bound params) {
        String content = message.signedContent();
        
        CompletableFuture<ModerationResult> checkChain = ModerationResult.safeFuture();
        for (ModerationChecker checker : checkers) {
            checkChain = checkChain.thenCompose(result -> {
                if (result.blocked()) return CompletableFuture.completedFuture(result);
                return checker.check(player, content);
            });
        }

        return checkChain.thenApply(result -> {
            if (result.blocked()) {
                String timestamp = LocalDateTime.now().format(LOG_FORMATTER);
                player.sendSystemMessage(Component.literal("§cYour message was blocked by moderation."));
                
                LOGGER.info("[{}] Blocked ({}) from {}: {}", timestamp, result.category(), player.getName().getString(), content);
                
                // Log to history for /logs command
                HistoryManager.logBlock(player.getName().getString(), player.getUUID(), content, result.category());
                
                return false;
            }

            // Allowed - Mark as moderated to prevent loop
            moderatedMessages.add(message);
            
            // CRITICAL: Re-broadcast MUST happen on the main server thread
            player.server.execute(() -> {
                player.server.getPlayerList().broadcastChatMessage(message, player, params);
                
                // Clean up old references to prevent memory leak
                if (moderatedMessages.size() > 500) {
                    moderatedMessages.clear(); 
                }
            });
            
            return true;
        });
    }

    public boolean isModerated(PlayerChatMessage message) {
        // Check by object identity
        return moderatedMessages.contains(message);
    }
}
