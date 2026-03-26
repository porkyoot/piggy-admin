package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.util.IgniterAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Creeper.class)
public abstract class CreeperMixin implements IgniterAccessor {
    @Unique
    private ServerPlayer piggy$igniter;

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Creeper;ignite()V"))
    private void onIgnite(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        System.out.println("[PiggyAdmin] Creeper ignited by player: " + player.getName().getString());
        if (player instanceof ServerPlayer serverPlayer) {
            this.piggy$igniter = serverPlayer;
        }
    }

    @Override
    public void piggy$setIgniter(@Nullable ServerPlayer player) {
        this.piggy$igniter = player;
    }

    @Override
    @Nullable
    public ServerPlayer piggy$getIgniter() {
        return this.piggy$igniter;
    }
}
