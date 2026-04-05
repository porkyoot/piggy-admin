package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.util.FireBlameManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireChargeItem.class)
public class FireChargeItemMixin {

    @Inject(method = "useOn", at = @At("RETURN"))
    private void onFireChargeUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            BlockPos pos = context.getClickedPos();
            boolean isTnt = player.level().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.TNT);
            BlockPos firePos = isTnt ? pos : pos.relative(context.getClickedFace());
            
            String worldId = player.serverLevel().dimension().location().toString();
            String blockPosStr = String.format("%d, %d, %d", firePos.getX(), firePos.getY(), firePos.getZ());
            String playerPosStr = String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ());

            if (isTnt) {
                // TNT Ignition: Emit specialized THREAT event
                is.pig.minecraft.admin.telemetry.HazardousPlacementEvent event = new is.pig.minecraft.admin.telemetry.HazardousPlacementEvent(
                        player.getName().getString(),
                        "TNT (Fire Charge Ignition)",
                        blockPosStr,
                        worldId,
                        playerPosStr,
                        player.getServer().getTickCount(),
                        is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.THREAT
                );
                is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance().dispatch(event);
                is.pig.minecraft.admin.util.AdminNotifier.broadcastAdminEvent(event);
            } else {
                // Standard Fire: Use regular blame manager
                FireBlameManager.recordFire(player, firePos, player.getName().getString() + " used Fire Charge");
            }
        }
    }
}
