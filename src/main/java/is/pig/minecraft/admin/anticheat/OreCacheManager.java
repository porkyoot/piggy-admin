package is.pig.minecraft.admin.anticheat;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class OreCacheManager {

    public static final OreCacheManager INSTANCE = new OreCacheManager();

    private final ConcurrentHashMap<ChunkPos, List<BlockPos>> cache = new ConcurrentHashMap<>();

    public void clearCache() {
        cache.clear();
    }

    private OreCacheManager() {
    }

    public void registerEvents() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            scanAndCacheChunk(level, chunk);
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel) {
                removeCachedOre(pos);
            }
        });
    }

    public void scanAndCacheChunk(ServerLevel level, ChunkAccess chunk) {
        CompletableFuture.runAsync(() -> scanAndCacheChunkSync(level, chunk));
    }

    public void scanAndCacheChunkSync(ServerLevel level, ChunkAccess chunk) {
        boolean isOverworld = level.dimension().location().getPath().contains("overworld");
        boolean isNether = level.dimension().location().getPath().contains("nether");
        if (!isOverworld && !isNether && !level.dimension().location().getNamespace().equals("test")) {
            return;
        }

        int minY;
        int maxY;

        if (isOverworld) {
            minY = level.getMinBuildHeight();
            maxY = 15; // Below Y=16
        } else {
            minY = 8;
            maxY = 119;
        }

        List<BlockPos> oresFound = new ArrayList<>();
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        for (int x = startX; x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = startZ; z <= chunkPos.getMaxBlockZ(); z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    
                    if (isValuableOre(state)) {
                        oresFound.add(pos);
                    }
                }
            }
        }

        if (!oresFound.isEmpty()) {
            // Using CopyOnWriteArrayList so it is thread-safe for reads/removals
            cache.computeIfAbsent(chunkPos, k -> new CopyOnWriteArrayList<>()).addAll(oresFound);
        }
    }

    private boolean isValuableOre(BlockState state) {
        return state.is(Blocks.DIAMOND_ORE) || 
               state.is(Blocks.DEEPSLATE_DIAMOND_ORE) || 
               state.is(Blocks.ANCIENT_DEBRIS);
    }

    private void removeCachedOre(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        List<BlockPos> ores = cache.get(chunkPos);
        if (ores != null) {
            ores.remove(pos);
        }
    }

    public List<BlockPos> getOresInRadius(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> result = new ArrayList<>();
        int radiusSq = radius * radius;

        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkPos chunkPos = new ChunkPos(cx, cz);
                List<BlockPos> chunkOres = cache.get(chunkPos);
                
                if (chunkOres != null) {
                    for (BlockPos pos : chunkOres) {
                        if (pos.distSqr(center) <= radiusSq) {
                            result.add(pos);
                        }
                    }
                }
            }
        }

        return result;
    }
}
