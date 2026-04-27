package is.pig.minecraft.admin.api;

import java.nio.file.Path;

public interface IPlatformEnvironment {
    Path getConfigDirectory();
    boolean isClient();
    boolean isDedicatedServer();
}
