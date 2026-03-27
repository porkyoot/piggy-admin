package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.BlameData;
import is.pig.minecraft.admin.storage.HistoryManager;
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

    @Inject(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private void onFireChargeUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            String worldId = player.serverLevel().dimension().location().toString();
            String action = player.getName().getString() + " used Fire Charge";

            BlameData blame = new BlameData(player.getUUID(), player.getName().getString(), action, worldId, pos);
            HistoryManager.logFire(player, blame);
        }
    }
}
