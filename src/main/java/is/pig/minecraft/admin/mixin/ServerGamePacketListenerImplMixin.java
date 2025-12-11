package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.admin.util.AdminNotifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleSignUpdate", at = @At(value = "RETURN"))
    private void onSignUpdate(ServerboundSignUpdatePacket packet, CallbackInfo ci) {
        if (this.player == null || this.player.serverLevel() == null) return;

        String[] lines = packet.getLines();
        String signText = String.join(" | ", lines);

        // Ignore empty signs
        if (signText.replace("|", "").trim().isEmpty()) return;
        
        BlockPos pos = packet.getPos();
        String worldId = this.player.serverLevel().dimension().location().toString();

        // 1. Log to History
        HistoryManager.logSign(this.player.getName().getString(), this.player.getUUID(), signText, worldId, pos);

        // 2. Notify Admins with "SIGN" tag
        AdminNotifier.notifyAdmins(
            this.player,                                        
            "SIGN",                                    
            pos,                                    
            Component.literal(signText) 
        );
    }
}