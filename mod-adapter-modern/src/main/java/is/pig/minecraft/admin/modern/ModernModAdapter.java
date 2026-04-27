package is.pig.minecraft.admin.modern;

import is.pig.minecraft.admin.api.IModAdapter;
import is.pig.minecraft.admin.api.IInventoryManager;
import is.pig.minecraft.admin.api.IPlayerTracker;
import is.pig.minecraft.admin.api.INetworkDispatcher;

public class ModernModAdapter implements IModAdapter {
    private final IInventoryManager inventoryManager = new ModernInventoryManager();

    @Override
    public IInventoryManager getInventoryManager() {
        return inventoryManager;
    }

    @Override
    public IPlayerTracker getPlayerTracker() {
        return null;
    }

    @Override
    public INetworkDispatcher getNetworkDispatcher() {
        return null;
    }
}
