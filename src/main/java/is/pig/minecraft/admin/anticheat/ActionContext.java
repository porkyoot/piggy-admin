package is.pig.minecraft.admin.anticheat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record ActionContext(Level level, BlockPos pos, BlockState state) {
}
