package is.pig.minecraft.admin;

import is.pig.minecraft.admin.anticheat.XRayDetector;
import is.pig.minecraft.admin.network.SyncModerationPayload;
import is.pig.minecraft.admin.network.UpdateAdminConfigPayload;
import is.pig.minecraft.admin.storage.HistoryManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents; // Import
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
// import net.fabricmc.loader.api.FabricLoader;
// import net.minecraft.server.players.ServerOpListEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiggyAdmin implements ModInitializer {
    public static final String MOD_ID = "piggy-admin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Ehlo from Piggy Admin!");

        is.pig.minecraft.admin.config.PiggyServerConfig.load();
        HistoryManager.load();

        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
                is.pig.minecraft.lib.network.SyncConfigPayload.TYPE,
                is.pig.minecraft.lib.network.SyncConfigPayload.CODEC);
        SyncModerationPayload.register();
        UpdateAdminConfigPayload.register();

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            // if (!server.getPlayerList().isOp(handler.getPlayer().getGameProfile())) {
            // server.getPlayerList().getOps().add(new
            // ServerOpListEntry(handler.getPlayer().getGameProfile(), 4, false));
            // server.getCommands().sendCommands(handler.getPlayer());
            // }
            // }

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
            // Intercept and handle asynchronously
            is.pig.minecraft.admin.moderation.ModerationEngine.getInstance().processMessage(sender, message, params);
            return false;
        });



        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender != null) {
                HistoryManager.logChat(sender, message.signedContent());
            }
        });

        // --- REGISTER XRAY DETECTOR ---
        is.pig.minecraft.admin.anticheat.IAntiCheatRule xrayDetector = new XRayDetector();
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide()) {
                xrayDetector.evaluate((net.minecraft.server.level.ServerPlayer) player, new is.pig.minecraft.admin.anticheat.ActionContext(world, pos, state));
            }
        });
    }
}