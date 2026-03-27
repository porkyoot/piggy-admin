package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.util.LavaBlameManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Inject(method = "flow", at = @At("HEAD"))
    private void onFlow(net.minecraft.world.level.LevelAccessor world, BlockPos to, BlockState state, Direction direction, FluidState fluidState, CallbackInfo ci) {
        if ((Object) this instanceof LavaFluid) {
            BlockPos from = to.relative(direction.getOpposite());
            LavaBlameManager.propagate(from, to);
        }
    }
}
