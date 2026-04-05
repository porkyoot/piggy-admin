package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.util.LavaBlameManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketItemMixin {

    @Inject(method = "emptyContents", at = @At("HEAD"))
    private void onEmptyContents(Player player, Level world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            BucketItem item = (BucketItem) (Object) this;
            // Check if it's a lava bucket (or any bucket that places lava)
            // In vanilla, BucketItem has a 'content' field (Fluid)
            // But we can just check if the item is Items.LAVA_BUCKET
            if (item.asItem() == Items.LAVA_BUCKET) {
                LavaBlameManager.recordLava(serverPlayer, pos);
            }
        }
    }
}
