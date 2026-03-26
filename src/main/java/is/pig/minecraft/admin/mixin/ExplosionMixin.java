package is.pig.minecraft.admin.mixin;

import is.pig.minecraft.admin.storage.HistoryEntry;
import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.admin.util.AdminNotifier;
import is.pig.minecraft.admin.util.IgniterAccessor;
import is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private Level level;
    
    @Shadow @Final @Nullable private DamageSource damageSource;
    @Shadow(aliases = {"exploder", "source", "entity"}) @Final @Nullable private Entity directEntity;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private float radius;

    @Unique private @Nullable Entity piggy$source;
    @Unique private double piggy$x;
    @Unique private double piggy$y;
    @Unique private double piggy$z;
    @Unique private float piggy$radius;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;DDDFLjava/util/List;Lnet/minecraft/world/level/Explosion$BlockInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)V", at = @At("RETURN"), require = 0)
    private void onInit(Level level, Entity entity, double x, double y, double z, float radius, List<BlockPos> affectedBlocks, Explosion.BlockInteraction interaction, ParticleOptions p1, ParticleOptions p2, Holder<?> h, CallbackInfo ci) {
        this.piggy$source = entity;
        this.piggy$x = x;
        this.piggy$y = y;
        this.piggy$z = z;
        this.piggy$radius = radius;
    }

    @Inject(method = "finalizeExplosion", at = @At("TAIL"))
    private void onFinalize(boolean particles, CallbackInfo ci) {
        if (!(this.level instanceof ServerLevel world)) return;

        // Determine effective values from either shadowed fields (preferred in 1.21) or constructor-captured ones
        Entity effectiveSource = (this.directEntity != null) ? this.directEntity : this.piggy$source;
        double effectiveX = (this.x != 0 || this.y != 0) ? this.x : this.piggy$x;
        double effectiveY = (this.x != 0 || this.y != 0) ? this.y : this.piggy$y;
        double effectiveZ = (this.x != 0 || this.y != 0) ? this.z : this.piggy$z;
        float effectiveRadius = (this.radius != 0) ? this.radius : this.piggy$radius;

        String causeName = "Unknown";
        ServerPlayer playerCause = null;

        if (effectiveSource != null) {
            causeName = effectiveSource.getType().getDescription().getString();
            if (effectiveSource instanceof ServerPlayer p) {
                playerCause = p;
            } else if (effectiveSource.getControllingPassenger() instanceof ServerPlayer p) {
                playerCause = p;
            } else if (effectiveSource instanceof TraceableEntity traceable && traceable.getOwner() instanceof ServerPlayer p) {
                playerCause = p;
                causeName = "Primed by " + p.getName().getString();
            } else if (effectiveSource instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer p) {
                playerCause = p;
                causeName = "Shot by " + p.getName().getString();
            } else if (effectiveSource instanceof PrimedTnt tnt && tnt.getOwner() instanceof ServerPlayer p) {
                playerCause = p;
                causeName = "TNT ignited by " + p.getName().getString();
            } else if (effectiveSource instanceof Creeper creeper) {
                if (creeper.isPowered()) {
                    causeName = "Charged Creeper";
                }
                if (creeper instanceof IgniterAccessor accessor && accessor.piggy$getIgniter() != null) {
                    playerCause = accessor.piggy$getIgniter();
                    causeName = (creeper.isPowered() ? "Charged Creeper" : "Creeper") + " ignited by " + playerCause.getName().getString();
                }
            }
        } else if (damageSource != null && damageSource.getEntity() instanceof ServerPlayer p) {
            playerCause = p;
            causeName = "Player: " + p.getName().getString();
        }

        BlockPos pos = new BlockPos((int) effectiveX, (int) effectiveY, (int) effectiveZ);
        String worldId = world.dimension().location().toString();
        String details = "Explosion (radius " + effectiveRadius + ") - Source: " + causeName;

        HistoryEntry entry;
        if (playerCause != null) {
            String fullAction = playerCause.getName().getString() + " caused Explosion - Source: " + causeName;
            is.pig.minecraft.admin.storage.BlameData blame = new is.pig.minecraft.admin.storage.BlameData(playerCause.getUUID(), playerCause.getName().getString(), fullAction, worldId, pos);
            entry = HistoryManager.logTnt(blame);
            AdminNotifier.notifyAdmins(playerCause, "EXPLOSION", pos, Component.literal(fullAction));
        } else {
            entry = HistoryManager.logExplosion(causeName, details, worldId, pos);
            // Also notify admins for non-player explosions to ensure visibility
            AdminNotifier.notifyAdmins(null, "EXPLOSION", pos, Component.literal("Natural Explosion - Source: " + causeName));
        }

        if (entry != null) {
            entry.putMetadata("radius", String.valueOf(effectiveRadius));
            
            List<ServerPlayer> nearbyPlayers = world.getPlayers(p -> p.distanceToSqr(effectiveX, effectiveY, effectiveZ) < 100 * 100);
            if (!nearbyPlayers.isEmpty()) {
                String formattedPlayers = nearbyPlayers.stream()
                    .map(p -> String.format("%s (%.1fm)", p.getName().getString(), Math.sqrt(p.distanceToSqr(effectiveX, effectiveY, effectiveZ))))
                    .collect(Collectors.joining(", "));
                entry.putMetadata("nearby_players", formattedPlayers);
                
                for (ServerPlayer p : nearbyPlayers) {
                    entry.putMetadata("player_data_" + p.getName().getString(), PiggyTelemetryFormatter.formatPlayer(p));
                }
            }

            List<ServerPlayer> victims = nearbyPlayers.stream()
                .filter(p -> p.isDeadOrDying() || p.deathTime > 0)
                .collect(Collectors.toList());
            
            if (!victims.isEmpty()) {
                String victimNames = victims.stream()
                    .map(p -> p.getName().getString())
                    .collect(Collectors.joining(", "));
                entry.putMetadata("victims", victimNames);
            }
            
            HistoryManager.save();
        }
    }
}
