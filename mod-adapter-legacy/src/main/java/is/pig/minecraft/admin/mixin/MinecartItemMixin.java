package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.legacy.LegacyMixinCallbacks;
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

    @Inject(method = "useOn", at = @At("RETURN"))
    private void onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            if (context.getItemInHand().is(net.minecraft.world.item.Items.TNT_MINECART)) {
                BlockPos pos = context.getClickedPos();
                String worldId = player.serverLevel().dimension().location().toString();
                String blockPosStr = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
                String playerPosStr = String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ());

                LegacyMixinCallbacks.trigger("hazardous_placement", player.getUUID(), player.getName().getString(), "TNT Minecart", worldId, pos.getX(), pos.getY(), pos.getZ(), player.getX(), player.getY(), player.getZ(), player.getServer().getTickCount());
            }
        }
    }
}
