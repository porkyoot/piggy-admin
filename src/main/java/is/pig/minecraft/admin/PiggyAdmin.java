package is.pig.minecraft.admin;

import is.pig.minecraft.admin.anticheat.XRayDetector;
import is.pig.minecraft.admin.network.SyncModerationPayload;
import is.pig.minecraft.admin.network.UpdateAdminConfigPayload;
import is.pig.minecraft.admin.storage.HistoryManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class PiggyAdmin implements ModInitializer {
    public static final String MOD_ID = "piggy-admin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        LOGGER.info("Ehlo from Piggy Admin!");

        is.pig.minecraft.admin.config.PiggyServerConfig.load();
        HistoryManager.init();
        is.pig.minecraft.admin.util.AdminNotifier.register();
        
        ServerLifecycleEvents.SERVER_STARTING.register(s -> {
            server = s;
            is.pig.minecraft.admin.moderation.WordListRegistry.initialize(is.pig.minecraft.admin.config.PiggyServerConfig.getInstance());
        });
        
        // Register administrative telemetry enrichers
        is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance()
            .registerEnricher(new is.pig.minecraft.admin.telemetry.AdminContextEnricher());

        // Register structured telemetry translators
        is.pig.minecraft.lib.util.telemetry.EventTranslatorRegistry.getInstance().register(
                is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.class,
                (event, i18n) -> {
                    var e = (is.pig.minecraft.admin.telemetry.HazardousPlacementEvent) event;
                    return switch (e.type()) {
                        case ARSON -> String.format("[ARSON] Player %s ignited %s at %s.", 
                                e.playerName(), e.targetName(), e.blockPos());
                        case HAZARD -> String.format("[HAZARD] Player %s spilled %s at %s.", 
                                e.playerName(), e.targetName(), e.blockPos());
                        case THREAT -> String.format("[THREAT] Player %s armed a %s charge at %s.", 
                                e.playerName(), e.targetName(), e.blockPos());
                    };
                }
        );

        is.pig.minecraft.lib.util.telemetry.EventTranslatorRegistry.getInstance().register(
                is.pig.minecraft.admin.telemetry.ExplosionDetonationEvent.class,
                (event, i18n) -> {
                    var e = (is.pig.minecraft.admin.telemetry.ExplosionDetonationEvent) event;
                    return String.format("[IMPACT] Detonation by %s at %s. Blast magnitude: %.1f. Loss: %d blocks.", 
                            e.sourceEntity(), e.blockPos(), e.explosionRadius(), e.blocksDestroyedCount());
                }
        );

        is.pig.minecraft.lib.util.telemetry.EventTranslatorRegistry.getInstance().register(
                is.pig.minecraft.admin.telemetry.ChatModerationEvent.class,
                (event, i18n) -> {
                    var e = (is.pig.minecraft.admin.telemetry.ChatModerationEvent) event;
                    return i18n.translate("piggy.admin.telemetry.chat_blocked", 
                            e.playerName(), e.moderationCategory(), e.confidenceScore());
                }
        );

        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
                is.pig.minecraft.lib.network.SyncConfigPayload.TYPE,
                is.pig.minecraft.lib.network.SyncConfigPayload.CODEC);
        SyncModerationPayload.register();
        UpdateAdminConfigPayload.register();

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            boolean allowCheats = is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().allowCheats;
            java.util.Map<String, Boolean> features = is.pig.minecraft.admin.config.PiggyServerConfig
                    .getInstance().features;
            is.pig.minecraft.lib.network.SyncConfigPayload payload = new is.pig.minecraft.lib.network.SyncConfigPayload(
                    allowCheats, features);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.getPlayer(), payload);
            
            // Sync moderation
            SyncModerationPayload modPayload = new SyncModerationPayload(
                is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().moderationEnabled,
                is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().moderationRules,
                is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().geminiApiKey,
                is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().geminiSystemPrompt,
                is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().geminiModel
            );
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.getPlayer(), modPayload);
        });

        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(UpdateAdminConfigPayload.TYPE, (payload, context) -> {
            if (!context.player().hasPermissions(2)) {
                LOGGER.warn("Illegal config update attempt from {}", context.player().getName().getString());
                return;
            }
            
            context.server().execute(() -> {
                is.pig.minecraft.admin.config.PiggyServerConfig config = is.pig.minecraft.admin.config.PiggyServerConfig.getInstance();
                config.allowCheats = payload.allowCheats();
                config.features.putAll(payload.features());
                config.moderationEnabled = payload.moderationEnabled();
                config.moderationRules.clear();
                config.moderationRules.addAll(payload.moderationRules());
                config.xrayCheck = payload.xrayCheck();
                config.xrayMaxRatio = payload.xrayMaxRatio();
                config.xrayMinBlocks = payload.xrayMinBlocks();
                config.geminiApiKey = payload.geminiApiKey();
                config.geminiSystemPrompt = payload.geminiSystemPrompt();
                config.geminiModel = payload.geminiModel();
                config.wordListLanguages.clear();
                config.wordListLanguages.putAll(payload.wordListLanguages());
                config.wordListEnabled = payload.wordListEnabled();
                config.wordListCacheDays = payload.wordListCacheDays();
                config.wordListFetchTimeoutSeconds = payload.wordListFetchTimeoutSeconds();
                
                is.pig.minecraft.admin.config.PiggyServerConfig.save();
                is.pig.minecraft.admin.moderation.ModerationEngine.getInstance().reload();
                
                // Broadcast updates to all clients
                is.pig.minecraft.lib.network.SyncConfigPayload cheatPayload = new is.pig.minecraft.lib.network.SyncConfigPayload(
                    config.allowCheats, config.features
                );
                SyncModerationPayload modPayload = new SyncModerationPayload(
                    config.moderationEnabled, config.moderationRules,
                    config.geminiApiKey, config.geminiSystemPrompt, config.geminiModel
                );
                
                for (var player : context.server().getPlayerList().getPlayers()) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, cheatPayload);
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, modPayload);
                }
                
                LOGGER.info("Admin config updated and propagated by {}", context.player().getName().getString());
            });
        });

        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) -> {
                    is.pig.minecraft.admin.command.PiggyAdminCommand.register(dispatcher);
                    is.pig.minecraft.admin.command.PiggyLogCommand.register(dispatcher);
                });

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender == null || !is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().moderationEnabled) {
                return true;
            }

            if (is.pig.minecraft.admin.moderation.ModerationEngine.getInstance().isModerated(message)) {
                return true;
            }

            LOGGER.info("Intercepting message for moderation: {}", message.signedContent());
            is.pig.minecraft.admin.moderation.ModerationEngine.getInstance().processMessage(sender, message, params);
            return false;
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender != null) {
                HistoryManager.logChat(sender, message.signedContent());
            }
        });

        is.pig.minecraft.admin.anticheat.OreCacheManager.INSTANCE.registerEvents();
        is.pig.minecraft.admin.anticheat.IAntiCheatRule xrayDetector = new XRayDetector();
        is.pig.minecraft.admin.anticheat.IAntiCheatRule hybridDetector = new is.pig.minecraft.admin.anticheat.HybridXRayDetector();
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide()) {
                is.pig.minecraft.admin.anticheat.ActionContext actionContext = new is.pig.minecraft.admin.anticheat.ActionContext(world, pos, state);
                xrayDetector.evaluate((net.minecraft.server.level.ServerPlayer) player, actionContext);
                if (is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().xrayHybridCheck) {
                    hybridDetector.evaluate((net.minecraft.server.level.ServerPlayer) player, actionContext);
                }
            }
        });
    }

    /**
     * @return The active MinecraftServer instance, or null if not yet started.
     */
    public static MinecraftServer getServer() {
        return server;
    }
}