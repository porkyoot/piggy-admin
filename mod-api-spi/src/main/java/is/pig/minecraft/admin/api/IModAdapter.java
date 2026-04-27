package is.pig.minecraft.admin.api;

public interface IModAdapter {
    IInventoryManager getInventoryManager();
    IPlayerTracker getPlayerTracker();
    INetworkDispatcher getNetworkDispatcher();
}
