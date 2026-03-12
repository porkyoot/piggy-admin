package is.pig.minecraft.admin.network;

import is.pig.minecraft.admin.config.PiggyServerConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record UpdateAdminConfigPayload(
        boolean allowCheats,
        Map<String, Boolean> features,
        boolean moderationEnabled,
        List<PiggyServerConfig.ModerationRule> moderationRules,
        boolean xrayCheck,
        float xrayMaxRatio,
        int xrayMinBlocks
) implements CustomPacketPayload {
    public static final Type<UpdateAdminConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("piggy-admin", "update_admin_config"));

    public static final StreamCodec<FriendlyByteBuf, UpdateAdminConfigPayload> CODEC = CustomPacketPayload.codec(
            UpdateAdminConfigPayload::write,
            UpdateAdminConfigPayload::new);

    public UpdateAdminConfigPayload(FriendlyByteBuf buf) {
        this(
                buf.readBoolean(),
                buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readBoolean),
                buf.readBoolean(),
                buf.readList(b -> {
                    String category = b.readUtf();
                    String language = b.readUtf();
                    String regex = b.readUtf();
                    boolean enabledRule = b.readBoolean();
                    PiggyServerConfig.ModerationRule rule = new PiggyServerConfig.ModerationRule(category, language, regex);
                    rule.enabled = enabledRule;
                    return rule;
                }),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(allowCheats);
        buf.writeMap(features, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeBoolean);
        buf.writeBoolean(moderationEnabled);
        buf.writeCollection(moderationRules, (b, rule) -> {
            b.writeUtf(rule.category);
            b.writeUtf(rule.language);
            b.writeUtf(rule.regex);
            b.writeBoolean(rule.enabled);
        });
        buf.writeBoolean(xrayCheck);
        buf.writeFloat(xrayMaxRatio);
        buf.writeInt(xrayMinBlocks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
    }
}
