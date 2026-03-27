package is.pig.minecraft.admin.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {
    private static final ThreadLocal<DispenserBlockEntity> piggy$tickingDispenser = new ThreadLocal<>();

    public static DispenserBlockEntity piggy$getCurrentDispenser() {
        return piggy$tickingDispenser.get();
    }

    @Inject(method = "dispense", at = @At("HEAD"))
    private void onDispenseHead(ServerLevel world, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (world.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser) {
            piggy$tickingDispenser.set(dispenser);
        }
    }

    @Inject(method = "dispense", at = @At("TAIL"))
    private void onDispenseTail(ServerLevel world, BlockState state, BlockPos pos, CallbackInfo ci) {
        piggy$tickingDispenser.remove();
    }
}
