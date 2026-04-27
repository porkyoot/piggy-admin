package is.pig.minecraft.admin.api;

import java.util.UUID;

public interface INetworkDispatcher {
    void sendToClient(UUID playerUuid, String channel, byte[] data);
    void broadcastToClients(String channel, byte[] data);
    void sendToServer(String channel, byte[] data);
}
