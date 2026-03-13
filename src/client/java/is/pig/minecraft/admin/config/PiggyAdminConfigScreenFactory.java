package is.pig.minecraft.admin.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import is.pig.minecraft.lib.features.CheatFeature;
import is.pig.minecraft.lib.features.CheatFeatureRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PiggyAdminConfigScreenFactory {

    public static Screen create(Screen parent) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Piggy Admin Configuration"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Chat Moderation"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Enable Moderation"))
                                .description(OptionDescription.of(Component.literal("Enable asynchronous chat moderation using regex rules.")))
                                .binding(false, () -> config.moderationEnabled, v -> config.moderationEnabled = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .group(ListOption.<String>createBuilder()
                                .name(Component.literal("Moderation Rules"))
                                .description(OptionDescription.of(Component.literal("Format: Category|Language|Regex (e.g., SWEARS|en|(?i)badword)")))
                                .binding(
                                        config.moderationRules.stream().map(r -> r.category.name() + "|" + r.language + "|" + r.regex).toList(),
                                        () -> config.moderationRules.stream().map(r -> r.category.name() + "|" + r.language + "|" + r.regex).toList(),
                                        v -> {
                                            config.moderationRules.clear();
                                            for (String s : v) {
                                                String[] parts = s.split("\\|", 3);
                                                if (parts.length == 3) {
                                                    is.pig.minecraft.admin.moderation.ModerationCategory category = is.pig.minecraft.admin.moderation.ModerationCategory.fromString(parts[0]);
                                                    config.moderationRules.add(new PiggyServerConfig.ModerationRule(category, parts[1], parts[2]));
                                                } else if (parts.length == 1 && !parts[0].isEmpty()) {
                                                     config.moderationRules.add(new PiggyServerConfig.ModerationRule(is.pig.minecraft.admin.moderation.ModerationCategory.OTHER, "all", parts[0]));
                                                }
                                            }
                                        }
                                )
                                .controller(StringControllerBuilder::create)
                                .initial("")
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Gemini AI Moderation"))
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Gemini API Key"))
                                        .description(OptionDescription.of(Component.literal("Your Gemini API Key from Google AI Studio.")))
                                        .binding("YOUR_GEMINI_API_KEY_HERE", () -> config.geminiApiKey, v -> config.geminiApiKey = v)
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("System Prompt"))
                                        .description(OptionDescription.of(Component.literal("Instruction for the AI moderator.")))
                                        .binding("", () -> config.geminiSystemPrompt, v -> config.geminiSystemPrompt = v)
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Model Name"))
                                        .binding("gemini-2.0-flash", () -> config.geminiModel, v -> config.geminiModel = v)
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Anti-Cheat"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Allow Cheats (Global)"))
                                .description(OptionDescription.of(Component.literal("Master switch for all cheat features on the server.")))
                                .binding(true, () -> config.allowCheats, v -> config.allowCheats = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("X-Ray Detector"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Enabled"))
                                        .binding(true, () -> config.xrayCheck, v -> config.xrayCheck = v)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Component.literal("Max Ratio"))
                                        .description(OptionDescription.of(Component.literal("Max ratio of rare blocks vs total blocks before flagging.")))
                                        .binding(0.15f, () -> config.xrayMaxRatio, v -> config.xrayMaxRatio = v)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.01f, 1.0f).step(0.01f))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Min Blocks"))
                                        .description(OptionDescription.of(Component.literal("Minimum blocks to mine before checking ratio.")))
                                        .binding(20, () -> config.xrayMinBlocks, v -> config.xrayMinBlocks = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(5, 100).step(1))
                                        .build())
                                .build())
                        .group(createFeaturesGroup(config))
                        .build())

                .save(() -> {
                    PiggyServerConfig.save();
                    if (net.minecraft.client.Minecraft.getInstance().level != null && 
                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(is.pig.minecraft.admin.network.UpdateAdminConfigPayload.TYPE)) {
                        
                        is.pig.minecraft.admin.network.UpdateAdminConfigPayload payload = new is.pig.minecraft.admin.network.UpdateAdminConfigPayload(
                            config.allowCheats,
                            config.features,
                            config.moderationEnabled,
                            config.moderationRules,
                            config.xrayCheck,
                            config.xrayMaxRatio,
                            config.xrayMinBlocks,
                            config.geminiApiKey,
                            config.geminiSystemPrompt,
                            config.geminiModel
                        );
                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                    }
                    is.pig.minecraft.admin.moderation.ModerationEngine.getInstance().reload();
                })


                .build()
                .generateScreen(parent);
    }

    private static OptionGroup createFeaturesGroup(PiggyServerConfig config) {
        OptionGroup.Builder builder = OptionGroup.createBuilder()
                .name(Component.literal("Specific Features"));

        for (CheatFeature feature : CheatFeatureRegistry.getAllFeatures()) {
            builder.option(Option.<Boolean>createBuilder()
                    .name(Component.literal(feature.displayName()))
                    .binding(feature.defaultEnabled(), 
                             () -> config.features.getOrDefault(feature.id(), feature.defaultEnabled()), 
                             v -> config.features.put(feature.id(), v))
                    .controller(TickBoxControllerBuilder::create)
                    .build());
        }

        return builder.build();
    }
}

