package is.pig.minecraft.admin.mixin;

import net.minecraft.core.BlockPos;
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
        BlockPos pos = packet.getPos();

        // Route through ModerationEngine for centralized logging and filtering
        is.pig.minecraft.admin.moderation.ModerationEngine.getInstance().processSign(this.player, lines, pos);
    }
}