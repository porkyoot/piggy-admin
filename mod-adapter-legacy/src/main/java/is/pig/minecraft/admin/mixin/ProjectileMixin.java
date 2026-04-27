package is.pig.minecraft.admin.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(Projectile.class)
public class ProjectileMixin {
    @Unique
    private UUID piggy$ownerUuid;
    @Unique
    private String piggy$ownerName;

    public void piggy$setOwner(UUID uuid, String name) {
        this.piggy$ownerUuid = uuid;
        this.piggy$ownerName = name;
    }

    public UUID piggy$getOwnerUuid() {
        return piggy$ownerUuid;
    }

    public String piggy$getOwnerName() {
        return piggy$ownerName;
    }
}
