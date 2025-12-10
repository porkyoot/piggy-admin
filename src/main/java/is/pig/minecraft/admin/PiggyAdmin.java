package is.pig.minecraft.admin;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiggyAdmin implements ModInitializer {
	public static final String MOD_ID = "piggy-admin";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Ehlo from Piggy Admin!");

		is.pig.minecraft.admin.config.PiggyServerConfig.load();

		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
				is.pig.minecraft.lib.network.SyncConfigPayload.TYPE,
				is.pig.minecraft.lib.network.SyncConfigPayload.CODEC);

		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			boolean allowCheats = is.pig.minecraft.admin.config.PiggyServerConfig.getInstance().allowCheats;
			java.util.Map<String, Boolean> features = is.pig.minecraft.admin.config.PiggyServerConfig
					.getInstance().features;
			is.pig.minecraft.lib.network.SyncConfigPayload payload = new is.pig.minecraft.lib.network.SyncConfigPayload(allowCheats, features);
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.getPlayer(), payload);
			LOGGER.info("[ANTI-CHEAT DEBUG] Sent SyncConfigPayload to player {} on join: allowCheats={}, features={}", handler.getPlayer().getName().getString(), allowCheats, features);
		});

		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT
				.register((dispatcher, registryAccess, environment) -> {
					is.pig.minecraft.admin.command.PiggyAdminCommand.register(dispatcher);
				});
	}
}