package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.admin.util.FireBlameManager;
import is.pig.minecraft.admin.util.IDispenserBlame;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(SmallFireball.class)
public abstract class SmallFireballMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void onConstructor(EntityType<? extends SmallFireball> entityType, Level world, CallbackInfo ci) {
        DispenserBlockEntity dispenser = DispenserBlockMixin.piggy$getCurrentDispenser();
        if (dispenser != null) {
            IDispenserBlame blame = (IDispenserBlame) dispenser;
            if (blame.piggy$getLastPlayerUuid() != null) {
                ((ProjectileMixin) (Object) this).piggy$setOwner(
                    blame.piggy$getLastPlayerUuid(), 
                    blame.piggy$getLastPlayerName()
                );
            }
        }
    }

    @Inject(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private void onFireballHit(BlockHitResult hitResult, CallbackInfo ci) {
        SmallFireball fireball = (SmallFireball) (Object) this;
        ProjectileMixin projectileMixin = (ProjectileMixin) (Object) fireball;
        
        UUID ownerUuid = projectileMixin.piggy$getOwnerUuid();
        String ownerName = projectileMixin.piggy$getOwnerName();

        if (ownerUuid != null) {
            BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());
            FireBlameManager.setOwner(pos, ownerUuid);
            
            String worldId = fireball.level().dimension().location().toString();
            if (fireball.level().getServer() != null) {
                HistoryManager.logDispenserFire(fireball.level().getServer(), ownerUuid, ownerName, worldId, pos);
            }
        }
    }
}
