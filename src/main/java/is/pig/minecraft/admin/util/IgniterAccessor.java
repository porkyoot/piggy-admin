package is.pig.minecraft.admin.util;
import is.pig.minecraft.api.*;

import org.jetbrains.annotations.Nullable;

public interface IgniterAccessor {
    void piggy$setIgniter(@Nullable Object player);
    @Nullable Object piggy$getIgniter();
}
