package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.util.IDispenserBlame;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(DispenserBlockEntity.class)
public abstract class DispenserBlockEntityMixin implements IDispenserBlame {

    @Unique
    private UUID piggy$lastPlayerUuid;
    @Unique
    private String piggy$lastPlayerName;

    @Override
    public UUID piggy$getLastPlayerUuid() {
        return piggy$lastPlayerUuid;
    }

    @Override
    public String piggy$getLastPlayerName() {
        return piggy$lastPlayerName;
    }

    @Inject(method = "stillValid", at = @At("HEAD"))
    private void onStillValid(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!player.level().isClientSide) {
            this.piggy$lastPlayerUuid = player.getUUID();
            this.piggy$lastPlayerName = player.getName().getString();
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void onLoadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        if (nbt.contains("PiggyLastPlayerUuid")) {
            this.piggy$lastPlayerUuid = nbt.getUUID("PiggyLastPlayerUuid");
        }
        if (nbt.contains("PiggyLastPlayerName")) {
            this.piggy$lastPlayerName = nbt.getString("PiggyLastPlayerName");
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        if (this.piggy$lastPlayerUuid != null) {
            nbt.putUUID("PiggyLastPlayerUuid", this.piggy$lastPlayerUuid);
        }
        if (this.piggy$lastPlayerName != null) {
            nbt.putString("PiggyLastPlayerName", this.piggy$lastPlayerName);
        }
    }
}
