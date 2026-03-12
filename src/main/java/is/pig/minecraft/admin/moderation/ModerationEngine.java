package is.pig.minecraft.admin.moderation;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.ChatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModerationEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModerationEngine");
    private static ModerationEngine INSTANCE;
    private final List<ModerationChecker> checkers = new ArrayList<>();

    public ModerationEngine() {
        checkers.add(new RegexModerationChecker());
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
    }

    private final java.util.Set<Integer> moderatedMessages = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Checks a message and takes action if needed.
     * @return A CompletableFuture that completes with true if the message should be ALLOWED, false if BLOCKED.
     */
    public CompletableFuture<Boolean> processMessage(ServerPlayer player, PlayerChatMessage message, ChatType.Bound params) {
        String content = message.signedContent();
        LOGGER.info("Moderating message from {}: {}", player.getName().getString(), content);

        
        List<CompletableFuture<Boolean>> futures = checkers.stream()
                .map(c -> c.check(player, content))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    for (CompletableFuture<Boolean> future : futures) {
                        if (future.join()) {
                            // Blocked
                            player.sendSystemMessage(Component.literal("§cYour message was blocked by moderation."));
                            LOGGER.info("Blocked message from {}: {}", player.getName().getString(), content);
                            return false; 
                        }
                    }
                    
                    // Allowed - re-broadcast
                    if (message.signature() != null) {
                        moderatedMessages.add(message.signature().hashCode());
                    }
                    player.server.getPlayerList().broadcastChatMessage(message, player, params);
                    return true;
                });
    }

    public boolean isModerated(PlayerChatMessage message) {
        if (message.signature() == null) return false;
        return moderatedMessages.contains(message.signature().hashCode());
    }
}

