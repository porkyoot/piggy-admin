package is.pig.minecraft.admin.storage;

import is.pig.minecraft.admin.moderation.ModerationCategory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class HistoryManager {
    private static final File HISTORY_FILE = FabricLoader.getInstance().getConfigDir().resolve("piggy-history.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static List<HistoryEntry> history = new ArrayList<>();

    public static HistoryEntry logTnt(BlameData blame) {
        return logTnt(null, blame);
    }

    public static HistoryEntry logTnt(net.minecraft.server.level.ServerPlayer player, BlameData blame) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                blame.authorName(),
                blame.authorUuid(),
                HistoryEntry.Type.TNT,
                blame.action()
        );
        entry.setLocation(blame.worldId(), blame.pos().getX(), blame.pos().getY(), blame.pos().getZ());
        history.add(entry);
        save();
        
        is.pig.minecraft.admin.util.AdminNotifier.notifyAdmins(player, "TNT", blame.pos(), net.minecraft.network.chat.Component.literal(blame.action()));
        return entry;
    }

    public static HistoryEntry logFire(BlameData blame) {
        return logFire(null, blame);
    }

    public static HistoryEntry logFire(net.minecraft.server.level.ServerPlayer player, BlameData blame) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                blame.authorName(),
                blame.authorUuid(),
                HistoryEntry.Type.FIRE,
                blame.action()
        );
        entry.setLocation(blame.worldId(), blame.pos().getX(), blame.pos().getY(), blame.pos().getZ());
        history.add(entry);
        save();
        
        is.pig.minecraft.admin.util.AdminNotifier.notifyAdmins(player, "FIRE", blame.pos(), net.minecraft.network.chat.Component.literal(blame.action()));
        return entry;
    }

    public static HistoryEntry logBurn(UUID authorUuid, String authorName, String blockName, String worldId, BlockPos pos) {
        return logBurn(authorUuid, authorName, blockName, worldId, pos, null);
    }

    public static HistoryEntry logBurn(UUID authorUuid, String authorName, String blockName, String worldId, BlockPos pos, net.minecraft.server.MinecraftServer server) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                authorName,
                authorUuid,
                HistoryEntry.Type.BURN,
                authorName + " burned " + blockName
        );
        entry.setLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        history.add(entry);
        save();
        
        net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(authorUuid);
        if (player != null) {
            is.pig.minecraft.admin.util.AdminNotifier.notifyAdmins(player, "BURN", pos, net.minecraft.network.chat.Component.literal(authorName + " burned " + blockName));
        }
        return entry;
    }

    public static HistoryEntry logExplosion(String source, String details, String worldId, BlockPos pos) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                source,
                null,
                HistoryEntry.Type.EXPLOSION,
                details
        );
        entry.setLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        history.add(entry);
        save();
        return entry;
    }

    public static List<HistoryEntry> findExplosionsAffecting(String worldId, BlockPos pos) {
        List<HistoryEntry> results = new ArrayList<>();
        List<HistoryEntry> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);

        for (HistoryEntry entry : reversed) {
            if ((entry.type == HistoryEntry.Type.EXPLOSION || entry.type == HistoryEntry.Type.TNT) &&
                entry.worldId != null && entry.worldId.equals(worldId)) {
                
                String radiusStr = entry.getMetadata().get("radius");
                float radius = radiusStr != null ? Float.parseFloat(radiusStr) : 4.0f; // Default TNT radius
                
                double dx = entry.x - pos.getX();
                double dy = entry.y - pos.getY();
                double dz = entry.z - pos.getZ();
                double distSq = dx*dx + dy*dy + dz*dz;
                
                if (distSq <= (radius + 1.5) * (radius + 1.5)) {
                    results.add(entry);
                }
            }
            // Limit to recent explosions for performance
            if (results.size() >= 5) break;
        }
        return results;
    }

    public static void load() {
        if (HISTORY_FILE.exists()) {
            try (FileReader reader = new FileReader(HISTORY_FILE)) {
                Type listType = new TypeToken<ArrayList<HistoryEntry>>(){}.getType();
                history = GSON.fromJson(reader, listType);
                if (history == null) history = new ArrayList<>();
            } catch (IOException e) {
                e.printStackTrace();
                history = new ArrayList<>();
            }
        }
    }

    public static void logLava(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        String worldId = player.serverLevel().dimension().location().toString();
        String action = "Placed Lava";
        BlameData blame = new BlameData(player.getUUID(), player.getName().getString(), action, worldId, pos);
        logLava(player, blame);
    }

    public static void logLava(net.minecraft.server.level.ServerPlayer player, BlameData blame) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                blame.authorName(),
                blame.authorUuid(),
                HistoryEntry.Type.LAVA,
                blame.action()
        );
        entry.setLocation(blame.worldId(), blame.pos().getX(), blame.pos().getY(), blame.pos().getZ());
        history.add(entry);
        save();

        is.pig.minecraft.admin.util.AdminNotifier.notifyAdmins(player, "LAVA", blame.pos(), net.minecraft.network.chat.Component.literal(blame.action()));
    }

    public static void logDispenserLava(net.minecraft.server.MinecraftServer server, UUID lastPlayerUuid, String lastPlayerName, String worldId, BlockPos pos) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                lastPlayerName,
                lastPlayerUuid,
                HistoryEntry.Type.LAVA,
                "Dispensed Lava (Last Editor: " + lastPlayerName + ")"
        );
        entry.setLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        history.add(entry);
        save();
        
        // Notify admins with "DISPENSER" tag using the new global notification system
        is.pig.minecraft.admin.util.AdminNotifier.notifyAdmins(server, lastPlayerName, "DISPENSER/LAVA", pos, worldId,
            net.minecraft.network.chat.Component.literal("Dispensed Lava - Last Editor: " + lastPlayerName));
    }

    public static void logDispenserFire(net.minecraft.server.MinecraftServer server, UUID lastPlayerUuid, String lastPlayerName, String worldId, BlockPos pos) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                lastPlayerName,
                lastPlayerUuid,
                HistoryEntry.Type.FIRE,
                "Dispensed Fire Charge (Last Editor: " + lastPlayerName + ")"
        );
        entry.setLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        history.add(entry);
        save();

        is.pig.minecraft.admin.util.AdminNotifier.notifyAdmins(server, lastPlayerName, "DISPENSER/FIRE", pos, worldId,
            net.minecraft.network.chat.Component.literal("Dispensed Fire Charge - Last Editor: " + lastPlayerName));
    }

    public static void logLavaBurn(UUID authorUuid, String authorName, String blockName, String worldId, BlockPos pos) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                authorName,
                authorUuid,
                HistoryEntry.Type.BURN,
                authorName + " burned " + blockName + " (via Lava)"
        );
        entry.setLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        history.add(entry);
        save();
        // No admin notification for spread damage to avoid spam
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
            GSON.toJson(history, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logChat(String playerName, UUID uuid, String message) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                playerName,
                uuid,
                HistoryEntry.Type.CHAT,
                message
        );
        history.add(entry);
        save();
    }

    public static void logChat(net.minecraft.server.level.ServerPlayer player, String message) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                player.getName().getString(),
                player.getUUID(),
                HistoryEntry.Type.CHAT,
                message
        );
        history.add(entry);
        save();
        
        // Chat isn't typically flagged to admins unless moderated, but we can log to console
    }

    public static void logBlock(net.minecraft.server.level.ServerPlayer player, String content, ModerationCategory category, String worldId, BlockPos pos) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                player.getName().getString(),
                player.getUUID(),
                HistoryEntry.Type.BLOCK,
                content
        ).setCategory(category);
        
        if (worldId != null && pos != null) {
            entry.setLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        }
        
        history.add(entry);
        save();
        
        is.pig.minecraft.admin.util.AdminNotifier.notifyAdmins(player, "BLOCKED", pos, 
            net.minecraft.network.chat.Component.literal("Blocked " + category + ": " + content));
    }

    public static void logSign(String playerName, UUID uuid, String text, String worldId, BlockPos pos) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                playerName,
                uuid,
                HistoryEntry.Type.SIGN,
                text
        );
        entry.setLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        history.add(entry);
        save();
    }

    public static void logSign(net.minecraft.server.level.ServerPlayer player, String text, String worldId, BlockPos pos) {
        HistoryEntry entry = new HistoryEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                player.getName().getString(),
                player.getUUID(),
                HistoryEntry.Type.SIGN,
                text
        );
        entry.setLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        history.add(entry);
        save();
        
        is.pig.minecraft.admin.util.AdminNotifier.notifyAdmins(player, "SIGN", pos, net.minecraft.network.chat.Component.literal(text));
    }

    public static List<HistoryEntry> getPlayerHistory(String playerName) {
        return history.stream()
                .filter(e -> e.playerName.equalsIgnoreCase(playerName))
                .collect(Collectors.toList());
    }

    public static HistoryEntry getSignInfo(String worldId, BlockPos pos) {
        return getBlockInfo(worldId, pos, HistoryEntry.Type.SIGN);
    }

    public static List<HistoryEntry> findEntriesNear(String worldId, BlockPos pos, double radius, HistoryEntry.Type... types) {
        List<HistoryEntry> results = new ArrayList<>();
        List<HistoryEntry> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);

        java.util.Set<HistoryEntry.Type> typeSet = java.util.EnumSet.noneOf(HistoryEntry.Type.class);
        for (HistoryEntry.Type t : types) typeSet.add(t);

        double radiusSq = radius * radius;

        for (HistoryEntry entry : reversed) {
            if (typeSet.contains(entry.type) && entry.worldId != null && entry.worldId.equals(worldId)) {
                double dx = entry.x - pos.getX();
                double dy = entry.y - pos.getY();
                double dz = entry.z - pos.getZ();
                double distSq = dx*dx + dy*dy + dz*dz;

                if (distSq <= radiusSq) {
                    results.add(entry);
                }
            }
            // Limit to recent entries for performance
            if (results.size() >= 50) break;
        }
        return results;
    }

    public static HistoryEntry getBlockInfo(String worldId, BlockPos pos, HistoryEntry.Type... types) {
        List<HistoryEntry> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        
        java.util.Set<HistoryEntry.Type> typeSet = java.util.EnumSet.noneOf(HistoryEntry.Type.class);
        for (HistoryEntry.Type t : types) typeSet.add(t);

        for (HistoryEntry entry : reversed) {
            if (typeSet.contains(entry.type) &&
                entry.worldId != null && entry.worldId.equals(worldId) &&
                entry.x == pos.getX() &&
                entry.y == pos.getY() &&
                entry.z == pos.getZ()) {
                return entry;
            }
        }
        return null;
    }

    // NEW METHOD: Returns a Set of all unique player names in the history
    public static Set<String> getKnownPlayerNames() {
        return history.stream()
                .map(entry -> entry.playerName)
                .collect(Collectors.toSet());
    }
}