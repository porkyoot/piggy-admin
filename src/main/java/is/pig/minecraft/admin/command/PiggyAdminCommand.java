package is.pig.minecraft.admin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import is.pig.minecraft.admin.config.PiggyServerConfig;
import is.pig.minecraft.lib.network.SyncConfigPayload;
import is.pig.minecraft.lib.features.CheatFeature;
import is.pig.minecraft.lib.features.CheatFeatureRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import java.util.Map;

public class PiggyAdminCommand {

    // Suggestion provider for feature IDs
    private static final SuggestionProvider<CommandSourceStack> FEATURE_SUGGESTIONS = (context, builder) -> {
        for (CheatFeature feature : CheatFeatureRegistry.getAllFeatures()) {
            builder.suggest(feature.id());
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("piggy")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("cheats")
                        .executes(PiggyAdminCommand::checkCheats)
                        .then(Commands.literal("allow")
                                .executes(ctx -> setCheats(ctx, true)))
                        .then(Commands.literal("forbid")
                                .executes(ctx -> setCheats(ctx, false))))
                .then(Commands.literal("feature")
                        .then(Commands.literal("list")
                                .executes(PiggyAdminCommand::listFeatures))
                        .then(Commands.argument("feature_id", StringArgumentType.word())
                                .suggests(FEATURE_SUGGESTIONS)
                                .then(Commands.literal("enable")
                                        .executes(ctx -> setFeature(ctx, true)))
                                .then(Commands.literal("disable")
                                        .executes(ctx -> setFeature(ctx, false)))
                                .executes(PiggyAdminCommand::checkFeature))));
    }

    private static int checkCheats(CommandContext<CommandSourceStack> context) {
        boolean allowed = PiggyServerConfig.getInstance().allowCheats;
        context.getSource().sendSuccess(
                () -> Component.literal("Piggy Cheats are currently: " + (allowed ? "ALLOWED" : "FORBIDDEN")), false);
        return 1;
    }

    private static int setCheats(CommandContext<CommandSourceStack> context, boolean allow) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();
        config.allowCheats = allow;
        PiggyServerConfig.save();

        context.getSource().sendSuccess(
                () -> Component.literal("Piggy Cheats are now: " + (allow ? "ALLOWED" : "FORBIDDEN")), true);

        // Sync to all players
        syncToAllPlayers(context);

        return 1;
    }

    private static int listFeatures(CommandContext<CommandSourceStack> context) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();

        context.getSource().sendSuccess(
                () -> Component.literal("=== Piggy Features ===").withStyle(ChatFormatting.GOLD), false);

        context.getSource().sendSuccess(
                () -> Component.literal("Master Switch: " + (config.allowCheats ? "ALLOWED" : "FORBIDDEN"))
                        .withStyle(config.allowCheats ? ChatFormatting.GREEN : ChatFormatting.RED),
                false);

        context.getSource().sendSuccess(
                () -> Component.literal(""), false);

        for (CheatFeature feature : CheatFeatureRegistry.getAllFeatures()) {
            boolean enabled = config.features.getOrDefault(feature.id(), feature.defaultEnabled());
            String status = enabled ? "ENABLED" : "DISABLED";
            ChatFormatting color = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;

            context.getSource().sendSuccess(
                    () -> Component.literal("• " + feature.displayName() + " (" + feature.id() + "): " + status)
                            .withStyle(color),
                    false);

            context.getSource().sendSuccess(
                    () -> Component.literal("  " + feature.description()).withStyle(ChatFormatting.GRAY), false);
        }

        return 1;
    }

    private static int checkFeature(CommandContext<CommandSourceStack> context) {
        String featureId = StringArgumentType.getString(context, "feature_id");

        CheatFeature feature = CheatFeatureRegistry.getFeature(featureId);
        if (feature == null) {
            context.getSource().sendFailure(
                    Component.literal("Unknown feature: " + featureId).withStyle(ChatFormatting.RED));
            return 0;
        }

        PiggyServerConfig config = PiggyServerConfig.getInstance();
        boolean enabled = config.features.getOrDefault(featureId, feature.defaultEnabled());

        context.getSource().sendSuccess(
                () -> Component.literal(feature.displayName() + " is currently: " + (enabled ? "ENABLED" : "DISABLED"))
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                false);

        return 1;
    }

    private static int setFeature(CommandContext<CommandSourceStack> context, boolean enable) {
        String featureId = StringArgumentType.getString(context, "feature_id");

        CheatFeature feature = CheatFeatureRegistry.getFeature(featureId);
        if (feature == null) {
            context.getSource().sendFailure(
                    Component.literal("Unknown feature: " + featureId).withStyle(ChatFormatting.RED));
            return 0;
        }

        PiggyServerConfig config = PiggyServerConfig.getInstance();
        config.features.put(featureId, enable);
        PiggyServerConfig.save();

        context.getSource().sendSuccess(
                () -> Component.literal(feature.displayName() + " is now: " + (enable ? "ENABLED" : "DISABLED"))
                        .withStyle(enable ? ChatFormatting.GREEN : ChatFormatting.RED),
                true);

        // Sync to all players
        syncToAllPlayers(context);

        return 1;
    }

    private static void syncToAllPlayers(CommandContext<CommandSourceStack> context) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();
        Map<String, Boolean> features = config.features;
        SyncConfigPayload payload = new SyncConfigPayload(config.allowCheats, features);

        for (var player : context.getSource().getServer().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
