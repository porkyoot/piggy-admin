package is.pig.minecraft.admin.anticheat;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface IAntiCheatRule {
    boolean evaluate(ServerPlayer player, ActionContext context);
}
