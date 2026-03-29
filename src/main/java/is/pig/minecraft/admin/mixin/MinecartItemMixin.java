package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.HistoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartItem.class)
public class MinecartItemMixin {

    @Shadow public AbstractMinecart.Type type;

    @Inject(method = "useOn", at = @At("RETURN"))
    private void onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            if (this.type == AbstractMinecart.Type.TNT) {
                BlockPos pos = context.getClickedPos();
                String worldId = player.serverLevel().dimension().location().toString();
                String action = player.getName().getString() + " placed TNT Minecart";

                is.pig.minecraft.admin.storage.BlameData blame = new is.pig.minecraft.admin.storage.BlameData(player.getUUID(), player.getName().getString(), action, worldId, pos);
                HistoryManager.logExplosion((net.minecraft.server.level.ServerPlayer) context.getPlayer(), blame, "TNT_MINECART");
            }
        }
    }
}
