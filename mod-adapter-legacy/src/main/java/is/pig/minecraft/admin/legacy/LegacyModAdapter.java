package is.pig.minecraft.admin.legacy;

import is.pig.minecraft.admin.api.IModAdapter;
import is.pig.minecraft.admin.api.IInventoryManager;
import is.pig.minecraft.admin.api.IPlayerTracker;
import is.pig.minecraft.admin.api.INetworkDispatcher;

public class LegacyModAdapter implements IModAdapter {
    private final IInventoryManager inventoryManager = new LegacyInventoryManager();

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
