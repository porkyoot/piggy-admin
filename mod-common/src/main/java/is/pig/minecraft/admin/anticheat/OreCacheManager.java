package is.pig.minecraft.admin.anticheat;

import is.pig.minecraft.admin.api.IWorldAnalyzer;
import is.pig.minecraft.admin.math.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages cached hidden ores for X-Ray detection.
 * Pure Java, zero Minecraft dependencies.
 */
public class OreCacheManager {

    public static final OreCacheManager INSTANCE = new OreCacheManager();

    private final ConcurrentHashMap<ChunkPos, List<Vector3>> cache = new ConcurrentHashMap<>();

    private OreCacheManager() {
    }

    public void clearCache() {
        cache.clear();
    }

    public void scanAndCacheChunkSync(IWorldAnalyzer analyzer, int chunkX, int chunkZ, boolean isOverworld, boolean isNether) {
        if (!isOverworld && !isNether) {
            return;
        }

        int minY;
        int maxY;

        if (isOverworld) {
            minY = -64; 
            maxY = 15;
        } else {
            minY = 8;
            maxY = 119;
        }

        List<Vector3> oresFound = new ArrayList<>();

        analyzer.iterateChunk(chunkX, chunkZ, (x, y, z, blockId) -> {
            if (y >= minY && y <= maxY && isValuableOre(blockId)) {
                oresFound.add(new Vector3(x, y, z));
            }
        });

        if (!oresFound.isEmpty()) {
            cache.computeIfAbsent(new ChunkPos(chunkX, chunkZ), k -> new CopyOnWriteArrayList<>()).addAll(oresFound);
        }
    }

    private boolean isValuableOre(String blockId) {
        return blockId.equals("minecraft:diamond_ore") || 
               blockId.equals("minecraft:deepslate_diamond_ore") || 
               blockId.equals("minecraft:ancient_debris");
    }

    public void removeCachedOre(int x, int y, int z) {
        ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
        List<Vector3> ores = cache.get(chunkPos);
        if (ores != null) {
            ores.remove(new Vector3(x, y, z));
        }
    }

    public List<Vector3> getOresInRadius(int centerX, int centerY, int centerZ, int radius) {
        List<Vector3> result = new ArrayList<>();
        int radiusSq = radius * radius;

        int minChunkX = (centerX - radius) >> 4;
        int maxChunkX = (centerX + radius) >> 4;
        int minChunkZ = (centerZ - radius) >> 4;
        int maxChunkZ = (centerZ + radius) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkPos chunkPos = new ChunkPos(cx, cz);
                List<Vector3> chunkOres = cache.get(chunkPos);
                
                if (chunkOres != null) {
                    for (Vector3 pos : chunkOres) {
                        double dx = pos.x() - centerX;
                        double dy = pos.y() - centerY;
                        double dz = pos.z() - centerZ;
                        if ((dx*dx + dy*dy + dz*dz) <= radiusSq) {
                            result.add(pos);
                        }
                    }
                }
            }
        }

        return result;
    }

    public static record ChunkPos(int x, int z) {}
}
