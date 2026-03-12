package is.pig.minecraft.admin.client;

import is.pig.minecraft.admin.config.PiggyAdminConfigScreenFactory;
import is.pig.minecraft.admin.config.PiggyServerConfig;
import is.pig.minecraft.admin.network.SyncModerationPayload;
import is.pig.minecraft.admin.network.UpdateAdminConfigPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

public class PiggyAdminClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SyncModerationPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                PiggyServerConfig config = PiggyServerConfig.getInstance();
                config.moderationEnabled = payload.enabled();
                config.moderationRules.clear();
                config.moderationRules.addAll(payload.rules());
                // ModerationEngine doesn't exist on client, but we keep config refreshed for UI
            });
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("piggy-client")
                .then(ClientCommandManager.literal("sync")

                    .executes(context -> {

                        if (ClientPlayNetworking.canSend(UpdateAdminConfigPayload.TYPE)) {
                            PiggyServerConfig config = PiggyServerConfig.getInstance();
                            UpdateAdminConfigPayload payload = new UpdateAdminConfigPayload(
                                config.allowCheats,
                                config.features,
                                config.moderationEnabled,
                                config.moderationRules,
                                config.xrayCheck,
                                config.xrayMaxRatio,
                                config.xrayMinBlocks
                            );
                            ClientPlayNetworking.send(payload);
                            context.getSource().sendFeedback(Component.literal("§aConfig synced to server."));
                        } else {
                            context.getSource().sendError(Component.literal("§cCannot sync: server does not support Piggy Admin updates or you are not an OP."));
                        }
                        return 1;
                    })
                )
            );
        });
    }
}

