package is.pig.minecraft.admin.storage;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public record BlameData(UUID authorUuid, String authorName, String action, String worldId, BlockPos pos) {
}
