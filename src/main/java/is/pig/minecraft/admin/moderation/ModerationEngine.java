package is.pig.minecraft.admin.moderation;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.ModerationAdapter;
import is.pig.minecraft.api.spi.ModerationChecker;
import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.lib.util.PiggyMessenger;
import is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher;
import is.pig.minecraft.admin.telemetry.ChatModerationEvent;
import is.pig.minecraft.admin.util.AdminNotifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Platform-agnostic moderation engine.
 * ZERO net.minecraft imports.
 */
public class ModerationEngine {
    private static ModerationEngine INSTANCE;
    private final List<ModerationChecker> checkers = new ArrayList<>();
    private final Set<Object> moderatedMessages = Collections.newSetFromMap(Collections.synchronizedMap(new java.util.IdentityHashMap<>()));

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
        WordListRegistry.refresh();
        moderatedMessages.clear();
    }

    public CompletableFuture<Boolean> processMessage(Object player, Object message, Object params) {
        ModerationAdapter adapter = PiggyServiceRegistry.getModerationAdapter();
        String content = adapter.getMessageContent(message);
        
        CompletableFuture<ModerationResult> checkChain = ModerationResult.safeFuture();
        for (ModerationChecker checker : checkers) {
            checkChain = checkChain.thenCompose(result -> {
                if (result.blocked()) return CompletableFuture.completedFuture(result);
                return checker.check(adapter.getPlayerUUID(player), content);
            });
        }

        return checkChain.thenApply(result -> {
            if (result.blocked()) {
                PiggyMessenger.sendError(player, "piggy.admin.moderation.blocked");
                
                ChatModerationEvent event = new ChatModerationEvent(
                        adapter.getPlayerName(player),
                        content,
                        result.category().toString(),
                        result.confidenceScore(),
                        "BLOCKED",
                        "N/A", // Coordinates can be added to adapter if needed
                        adapter.getServerTickCount(player)
                );
                StructuredEventDispatcher.getInstance().dispatch(event);
                AdminNotifier.broadcastAdminEvent(event);
                
                // Note: HistoryManager needs decoupling too, but for now we use Object player
                HistoryManager.logBlock(player, content, result.category(), "N/A", null);
                
                return false;
            }

            moderatedMessages.add(message);
            adapter.broadcastMessage(player, message, params);
            
            if (moderatedMessages.size() > 500) {
                moderatedMessages.clear(); 
            }
            
            return true;
        });
    }

    public CompletableFuture<Boolean> processSign(Object player, String[] lines, Object pos) {
        ModerationAdapter adapter = PiggyServiceRegistry.getModerationAdapter();
        String content = String.join(" | ", lines);
        if (content.replace("|", "").trim().isEmpty()) return CompletableFuture.completedFuture(true);

        CompletableFuture<ModerationResult> checkChain = ModerationResult.safeFuture();
        for (ModerationChecker checker : checkers) {
            checkChain = checkChain.thenCompose(result -> {
                if (result.blocked()) return CompletableFuture.completedFuture(result);
                return checker.check(adapter.getPlayerUUID(player), content);
            });
        }

        return checkChain.thenApply(result -> {
            if (result.blocked()) {
                PiggyMessenger.sendError(player, "piggy.admin.moderation.blocked");
                
                ChatModerationEvent event = new ChatModerationEvent(
                        adapter.getPlayerName(player),
                        content,
                        result.category().toString(),
                        result.confidenceScore(),
                        "BLOCKED_SIGN",
                        "N/A",
                        adapter.getServerTickCount(player)
                );
                StructuredEventDispatcher.getInstance().dispatch(event);
                AdminNotifier.broadcastAdminEvent(event);
                
                HistoryManager.logBlock(player, content, result.category(), "N/A", null);
                return false;
            }
            
            HistoryManager.logSign(player, content, "N/A", null);
            return true;
        });
    }

    public boolean isModerated(Object message) {
        return moderatedMessages.contains(message);
    }
}
