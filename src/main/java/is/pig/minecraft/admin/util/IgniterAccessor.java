package is.pig.minecraft.admin.util;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface IgniterAccessor {
    void piggy$setIgniter(@Nullable ServerPlayer player);
    @Nullable ServerPlayer piggy$getIgniter();
}
