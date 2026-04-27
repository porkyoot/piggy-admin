package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.admin.util.FireBlameManager;
import is.pig.minecraft.admin.util.LavaBlameManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import net.minecraft.world.level.block.Blocks;

@Mixin(FireBlock.class)
public class FireBlockMixin {
    @Unique private static final ThreadLocal<BlockPos> piggy$tickingFirePos = new ThreadLocal<>();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        piggy$tickingFirePos.set(pos);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        piggy$tickingFirePos.remove();
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 0)
    private boolean onLevelSetBlock(Level world, BlockPos pos, BlockState state, int flags) {
        if (world instanceof ServerLevel serverLevel) {
            handleBlockChange(serverLevel, pos, state);
        }
        return world.setBlock(pos, state, flags);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 0)
    private boolean onServerLevelSetBlock(ServerLevel world, BlockPos pos, BlockState state, int flags) {
        handleBlockChange(world, pos, state);
        return world.setBlock(pos, state, flags);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"), require = 0)
    private boolean onLevelSetBlock4(Level world, BlockPos pos, BlockState state, int flags, int recursion) {
        if (world instanceof ServerLevel serverLevel) {
            handleBlockChange(serverLevel, pos, state);
        }
        return world.setBlock(pos, state, flags, recursion);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"), require = 0)
    private boolean onLevelRemoveBlock(Level world, BlockPos pos, boolean move) {
        if (world instanceof ServerLevel serverLevel) {
            handleBlockChange(serverLevel, pos, Blocks.AIR.defaultBlockState());
        }
        return world.removeBlock(pos, move);
    }

    @Unique
    private void handleBlockChange(ServerLevel world, BlockPos pos, BlockState newState) {
        BlockPos sourcePos = piggy$tickingFirePos.get();
        if (sourcePos == null) return;

        UUID ownerUuid = FireBlameManager.getOwner(sourcePos);
        boolean fromLava = false;
        
        if (ownerUuid == null) {
            // Check if this fire was started by lava nearby
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = sourcePos.relative(dir);
                if (world.getFluidState(neighbor).is(net.minecraft.tags.FluidTags.LAVA)) {
                    ownerUuid = LavaBlameManager.getOwner(neighbor);
                    if (ownerUuid != null) {
                        fromLava = true;
                        break;
                    }
                }
            }
        }

        if (ownerUuid == null) return;

        BlockState oldState = world.getBlockState(pos);
        boolean wasBurnable = !oldState.isAir() && !oldState.is(Blocks.FIRE) && oldState.getFluidState().isEmpty();
        
        boolean isPropagating = newState.is(Blocks.FIRE);
        boolean isDestroying = newState.isAir();

        if (isPropagating || isDestroying) {
            String burnerName = world.getServer().getPlayerList().getPlayer(ownerUuid) != null ?
                    world.getServer().getPlayerList().getPlayer(ownerUuid).getName().getString() : "Unknown (Left)";
            
            if (wasBurnable) {
                if (fromLava) {
                    HistoryManager.logLavaBurn(ownerUuid, burnerName, oldState.getBlock().getName().getString(), world.dimension().location().toString(), pos);
                } else {
                    HistoryManager.logBurn(ownerUuid, burnerName, oldState.getBlock().getName().getString(), world.dimension().location().toString(), pos, world.getServer());
                }
            }

            if (isPropagating) {
                FireBlameManager.setOwner(pos, ownerUuid);
            }
        }
    }
}
