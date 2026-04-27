package is.pig.minecraft.admin.api;

/**
 * Pure Java interface for world analysis without exposing Minecraft classes.
 */
public interface IWorldAnalyzer {
    String getBlockId(int x, int y, int z);
    
    void iterateChunk(int chunkX, int chunkZ, ChunkBlockConsumer consumer);
    
    interface ChunkBlockConsumer {
        void accept(int x, int y, int z, String blockId);
    }
}
