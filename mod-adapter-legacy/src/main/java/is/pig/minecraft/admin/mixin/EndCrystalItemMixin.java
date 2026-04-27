package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.HistoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystalItem.class)
public class EndCrystalItemMixin {

    @Inject(method = "useOn", at = @At("RETURN"))
    private void onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            String worldId = player.serverLevel().dimension().location().toString();
            String blockPosStr = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            String playerPosStr = String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ());

            // 1. Log to legacy history for backward compatibility with /blame
            is.pig.minecraft.admin.storage.BlameData blame = new is.pig.minecraft.admin.storage.BlameData(
                player.getUUID(), player.getName().getString(), player.getName().getString() + " placed End Crystal", worldId, pos);
            HistoryManager.logExplosion(player, blame, "END_CRYSTAL");

            // 2. Emit structured telemetry event
            is.pig.minecraft.admin.telemetry.HazardousPlacementEvent event = new is.pig.minecraft.admin.telemetry.HazardousPlacementEvent(
                player.getName().getString(),
                "End Crystal",
                blockPosStr,
                worldId,
                playerPosStr,
                player.getServer().getTickCount(),
                is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.THREAT
            );
            is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance().dispatch(event);

            // 3. Trigger interactive admin notification
            is.pig.minecraft.admin.util.AdminNotifier.broadcastAdminEvent(event);
        }
    }
}
