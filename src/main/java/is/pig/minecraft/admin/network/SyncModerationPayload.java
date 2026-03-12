package is.pig.minecraft.admin.network;

import is.pig.minecraft.admin.config.PiggyServerConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;


public record SyncModerationPayload(boolean enabled, List<PiggyServerConfig.ModerationRule> rules) implements CustomPacketPayload {
    public static final Type<SyncModerationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("piggy-admin", "sync_moderation"));

    public static final StreamCodec<FriendlyByteBuf, SyncModerationPayload> CODEC = CustomPacketPayload.codec(
            SyncModerationPayload::write,
            SyncModerationPayload::new);

    public SyncModerationPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readList(b -> {
            String category = b.readUtf();
            String language = b.readUtf();
            String regex = b.readUtf();
            boolean enabledRule = b.readBoolean();
            PiggyServerConfig.ModerationRule rule = new PiggyServerConfig.ModerationRule(category, language, regex);
            rule.enabled = enabledRule;
            return rule;
        }));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeCollection(rules, (b, rule) -> {
            b.writeUtf(rule.category);
            b.writeUtf(rule.language);
            b.writeUtf(rule.regex);
            b.writeBoolean(rule.enabled);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    }
}
