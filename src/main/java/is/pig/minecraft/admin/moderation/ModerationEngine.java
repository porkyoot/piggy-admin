package is.pig.minecraft.admin.moderation;

import is.pig.minecraft.admin.storage.HistoryManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.ChatType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModerationEngine {
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
        is.pig.minecraft.admin.moderation.WordListRegistry.refresh();
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
                is.pig.minecraft.lib.util.PiggyMessenger.sendError(player, "piggy.admin.moderation.blocked");
                
                // Emit structured telemetry
                is.pig.minecraft.admin.telemetry.ChatModerationEvent event = new is.pig.minecraft.admin.telemetry.ChatModerationEvent(
                        player.getName().getString(),
                        content,
                        result.category().toString(),
                        result.confidenceScore(),
                        "BLOCKED",
                        String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ()),
                        player.getServer().getTickCount()
                );
                is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance().dispatch(event);
                is.pig.minecraft.admin.util.AdminNotifier.broadcastAdminEvent(event);
                
                // Log to history for /logs command
                HistoryManager.logBlock(player, content, result.category(), player.serverLevel().dimension().location().toString(), player.blockPosition());
                
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

    public CompletableFuture<Boolean> processSign(ServerPlayer player, String[] lines, BlockPos pos) {
        String content = String.join(" | ", lines);
        if (content.replace("|", "").trim().isEmpty()) return CompletableFuture.completedFuture(true);

        CompletableFuture<ModerationResult> checkChain = ModerationResult.safeFuture();
        for (ModerationChecker checker : checkers) {
            checkChain = checkChain.thenCompose(result -> {
                if (result.blocked()) return CompletableFuture.completedFuture(result);
                return checker.check(player, content);
            });
        }

        return checkChain.thenApply(result -> {
            if (result.blocked()) {
                is.pig.minecraft.lib.util.PiggyMessenger.sendError(player, "piggy.admin.moderation.blocked");
                
                // Emit structured telemetry for sign block
                is.pig.minecraft.admin.telemetry.ChatModerationEvent event = new is.pig.minecraft.admin.telemetry.ChatModerationEvent(
                        player.getName().getString(),
                        content,
                        result.category().toString(),
                        result.confidenceScore(),
                        "BLOCKED_SIGN",
                        String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ()),
                        player.getServer().getTickCount()
                );
                is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance().dispatch(event);
                is.pig.minecraft.admin.util.AdminNotifier.broadcastAdminEvent(event);
                
                HistoryManager.logBlock(player, content, result.category(), player.serverLevel().dimension().location().toString(), pos);
                return false;
            }
            
            // Log allowed sign to history
            HistoryManager.logSign(player, content, player.serverLevel().dimension().location().toString(), pos);
            return true;
        });
    }

    public boolean isModerated(PlayerChatMessage message) {
        // Check by object identity
        return moderatedMessages.contains(message);
    }
}
