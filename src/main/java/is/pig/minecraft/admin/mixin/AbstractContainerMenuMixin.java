package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.HistoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    private static final java.util.Map<java.util.UUID, Long> COOLDOWNS = new java.util.HashMap<>();

    @Inject(method = "clicked", at = @At("HEAD"))
    private void onClicked(int slotId, int button, ClickType clickType, net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!((Object) this instanceof DispenserMenu dispenserMenu)) return;

        // Deduplication: Cooldown per player
        long now = System.currentTimeMillis();
        if (COOLDOWNS.containsKey(serverPlayer.getUUID()) && (now - COOLDOWNS.get(serverPlayer.getUUID()) < 2000)) {
            return;
        }
        
        ItemStack carried = dispenserMenu.getCarried();
        Slot slot = slotId >= 0 && slotId < dispenserMenu.slots.size() ? dispenserMenu.getSlot(slotId) : null;
        
        ItemStack targetStack = null;
        if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
            if (!carried.isEmpty()) {
                targetStack = carried;
            } else if (slot != null && slot.hasItem()) {
                targetStack = slot.getItem();
            }
        }

        if (targetStack != null && isDangerous(targetStack)) {
            BlockPos pos = serverPlayer.blockPosition(); // Fallback
            String worldId = serverPlayer.serverLevel().dimension().location().toString();
            String itemName = targetStack.getItem().getName(targetStack).getString();
            String action = serverPlayer.getName().getString() + " interacted with " + itemName + " in Dispenser";

            COOLDOWNS.put(serverPlayer.getUUID(), now);
            is.pig.minecraft.admin.storage.BlameData blame = new is.pig.minecraft.admin.storage.BlameData(serverPlayer.getUUID(), serverPlayer.getName().getString(), action, worldId, pos);

            if (targetStack.is(Items.TNT) || targetStack.is(Items.TNT_MINECART)) {
                HistoryManager.logTnt(serverPlayer, blame);
            } else if (targetStack.is(Items.LAVA_BUCKET)) {
                HistoryManager.logLava(serverPlayer, blame);
            } else if (targetStack.is(Items.FIRE_CHARGE) || targetStack.is(Items.FLINT_AND_STEEL)) {
                HistoryManager.logFire(serverPlayer, blame);
            }
        }
    }

    private boolean isDangerous(ItemStack stack) {
        return stack.is(Items.TNT) || stack.is(Items.TNT_MINECART) || 
               stack.is(Items.LAVA_BUCKET) || stack.is(Items.FIRE_CHARGE) || 
               stack.is(Items.FLINT_AND_STEEL);
    }
}
