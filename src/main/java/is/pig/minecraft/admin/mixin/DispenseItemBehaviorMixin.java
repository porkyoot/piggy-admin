package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.admin.util.IDispenserBlame;
import is.pig.minecraft.admin.util.LavaBlameManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(DefaultDispenseItemBehavior.class)
public class DispenseItemBehaviorMixin {

    @Inject(method = "execute", at = @At("HEAD"))
    private void onExecute(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        DispenserBlockEntity dispenser = DispenserBlockMixin.piggy$getCurrentDispenser();
        if (dispenser == null) return;

        IDispenserBlame blame = (IDispenserBlame) dispenser;
        UUID ownerUuid = blame.piggy$getLastPlayerUuid();
        String ownerName = blame.piggy$getLastPlayerName();

        if (ownerUuid == null) return;

        BlockPos targetPos = source.pos().relative(source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING));
        String worldId = source.level().dimension().location().toString();

        if (stack.is(Items.LAVA_BUCKET)) {
            LavaBlameManager.setOwner(targetPos, ownerUuid);
            HistoryManager.logDispenserLava(source.level().getServer(), ownerUuid, ownerName, worldId, targetPos);
        } else if (stack.is(Items.FIRE_CHARGE)) {
            // FIRE_CHARGE behavior is usually projectle, but we might catch it here if custom.
        }
    }
}
