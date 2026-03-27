package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.BlameData;
import is.pig.minecraft.admin.storage.HistoryManager;
import net.minecraft.core.BlockPos;
import is.pig.minecraft.admin.util.FireBlameManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelItemMixin {

    @Inject(method = "useOn", at = @At("RETURN"))
    private void onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            String worldId = player.serverLevel().dimension().location().toString();
            String action = player.getName().getString() + " ignited fire";

            // Record initial blame
            FireBlameManager.setOwner(pos, player.getUUID());

            // Log to history (now also notifies admins)
            BlameData blame = new BlameData(player.getUUID(), player.getName().getString(), action, worldId, pos);
            HistoryManager.logFire(player, blame);
        }
    }
}
