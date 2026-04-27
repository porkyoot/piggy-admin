package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.legacy.LegacyMixinCallbacks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            Block block = ((BlockItem) (Object) this).getBlock();
            boolean isTnt = block == Blocks.TNT;
            boolean isBedExplosion = block instanceof net.minecraft.world.level.block.BedBlock && !player.serverLevel().dimensionType().bedWorks();

            if (isTnt || isBedExplosion) {
                BlockPos pos = context.getClickedPos();
                String worldId = player.serverLevel().dimension().location().toString();
                String action = player.getName().getString() + " placed " + block.getName().getString();

                LegacyMixinCallbacks.trigger("explosion_place", player.getUUID(), player.getName().getString(), action, worldId, pos.getX(), pos.getY(), pos.getZ(), isTnt ? "TNT" : "BED");
            }
        }
    }
}
