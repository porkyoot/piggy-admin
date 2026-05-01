package is.pig.minecraft.admin.anticheat;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.WorldStateAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Platform-agnostic ore cache manager for anti-cheat.
 * ZERO net.minecraft imports.
 */
public class OreCacheManager {

    public static final OreCacheManager INSTANCE = new OreCacheManager();

    private final ConcurrentHashMap<ChunkPos, List<BlockPos>> cache = new ConcurrentHashMap<>();

    private OreCacheManager() {}

    public void clearCache() {
        cache.clear();
    }

    /**
     * Scans a chunk for valuable ores and caches them. 
     * This should be called by the Core module when a chunk is loaded.
     */
    public void scanAndCacheChunk(String worldId, ChunkPos chunkPos) {
        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        List<BlockPos> oresFound = new ArrayList<>();
        
        int startX = chunkPos.x() << 4;
        int startZ = chunkPos.z() << 4;
        
        // Simplified scan logic using SPI
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                // Focus on common diamond/ancient debris layers
                for (int y = -64; y <= 128; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    String stateId = worldState.getBlockStateId(worldId, pos);
                    
                    if (isValuable(stateId)) {
                        oresFound.add(pos);
                    }
                }
            }
        }

        if (!oresFound.isEmpty()) {
            cache.computeIfAbsent(chunkPos, k -> new CopyOnWriteArrayList<>()).addAll(oresFound);
        }
    }

    private boolean isValuable(String stateId) {
        if (stateId == null) return false;
        return stateId.contains("diamond_ore") || stateId.contains("ancient_debris");
    }

    public void removeCachedOre(BlockPos pos) {
        cache.computeIfAbsent(ChunkPos.fromBlockPos(pos), k -> new CopyOnWriteArrayList<>()).remove(pos);
    }

    public List<BlockPos> getOresInRadius(BlockPos center, int radius) {
        List<BlockPos> result = new ArrayList<>();
        int radiusSq = radius * radius;

        int minChunkX = (center.x() - radius) >> 4;
        int maxChunkX = (center.x() + radius) >> 4;
        int minChunkZ = (center.z() - radius) >> 4;
        int maxChunkZ = (center.z() + radius) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                List<BlockPos> chunkOres = cache.get(new ChunkPos(cx, cz));
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
