package is.pig.minecraft.admin;

import is.pig.minecraft.admin.storage.HistoryManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader; // Import FabricLoader
import net.minecraft.server.players.ServerOpListEntry; // Import OpEntry
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiggyAdmin implements ModInitializer {
    public static final String MOD_ID = "piggy-admin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Ehlo from Piggy Admin!");

        is.pig.minecraft.admin.config.PiggyServerConfig.load();
        HistoryManager.load(); // Load history

        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
                is.pig.minecraft.lib.network.SyncConfigPayload.TYPE,
                is.pig.minecraft.lib.network.SyncConfigPayload.CODEC);

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // --- DEBUG: AUTO OP ---
            // Only runs in dev environment (VSCode), safe for production builds
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                if (!server.getPlayerList().isOp(handler.getPlayer().getGameProfile())) {
                    server.getPlayerList().getOps().add(new ServerOpListEntry(handler.getPlayer().getGameProfile(), 4, false));
                    // Refresh commands so they get OP permissions immediately
                    server.getCommands().sendCommands(handler.getPlayer());
                    LOGGER.info("[Debug] Auto-opped player: " + handler.getPlayer().getName().getString());
                }
            }
            // ----------------------

            boolean allowCheats = is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().allowCheats;
            java.util.Map<String, Boolean> features = is.pig.minecraft.admin.config.PiggyServerConfig
                    .getInstance().features;
            is.pig.minecraft.lib.network.SyncConfigPayload payload = new is.pig.minecraft.lib.network.SyncConfigPayload(allowCheats, features);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.getPlayer(), payload);
        });

        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) -> {
                    is.pig.minecraft.admin.command.PiggyAdminCommand.register(dispatcher);
                    is.pig.minecraft.admin.command.PiggyLogCommand.register(dispatcher);
                });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender != null) {
                HistoryManager.logChat(sender.getName().getString(), sender.getUUID(), message.signedContent());
            }
        });
    }
}