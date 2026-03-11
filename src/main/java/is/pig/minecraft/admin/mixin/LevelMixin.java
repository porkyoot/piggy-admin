package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.admin.util.AdminNotifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;", at = @At("HEAD"))
    private void onExplode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator calculator, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction explosionInteraction, CallbackInfoReturnable<Explosion> cir) {
        if (!((Object) this instanceof ServerLevel world)) return;

        String cause = "Unknown";
        ServerPlayer playerCause = null;

        if (entity != null) {
            cause = entity.getType().getDescription().getString();
            if (entity instanceof ServerPlayer p) {
                playerCause = p;
            } else if (entity.getControllingPassenger() instanceof ServerPlayer p) {
                playerCause = p;
            } else if (entity instanceof net.minecraft.world.entity.TraceableEntity traceable && traceable.getOwner() instanceof ServerPlayer p) {
                playerCause = p;
                cause = "Primed by " + p.getName().getString();
            } else if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() instanceof ServerPlayer p) {
                playerCause = p;
                cause = "Shot by " + p.getName().getString();
            }
        } else if (damageSource != null && damageSource.getEntity() instanceof ServerPlayer p) {
            playerCause = p;
            cause = "Player: " + p.getName().getString();
        }

        BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
        String worldId = world.dimension().location().toString();
        String details = "Explosion (radius " + radius + ") - Source: " + cause;

        if (playerCause != null) {
            String fullAction = playerCause.getName().getString() + " caused Explosion - Source: " + cause;
            HistoryManager.logTnt(playerCause.getName().getString(), playerCause.getUUID(), fullAction, worldId, pos);
            AdminNotifier.notifyAdmins(playerCause, "EXPLOSION", pos, Component.literal(fullAction));
        } else {
            HistoryManager.logExplosion(cause, details, worldId, pos);
        }
    }
}
