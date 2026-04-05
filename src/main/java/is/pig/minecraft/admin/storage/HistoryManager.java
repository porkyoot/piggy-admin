package is.pig.minecraft.admin.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import is.pig.minecraft.admin.PiggyAdmin;
import is.pig.minecraft.lib.util.telemetry.JsonHistoryStore;
import is.pig.minecraft.lib.util.telemetry.StructuredEvent;
import is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HistoryManager {
    private static final String MODERATION_FILE = "piggy-moderation.json";
    private static final String LEGACY_FILE = "piggy-history.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static JsonHistoryStore moderationStore;

    public static void init() {
        moderationStore = new JsonHistoryStore(MODERATION_FILE, event -> 
            event.getEventKey().startsWith("piggy.admin") || 
            event.getEventKey().contains("chat_moderation") ||
            event.getEventKey().contains("xray")
        );
        moderationStore.register();
        
        // Perform legacy migration
        migrateLegacyData();
    }

    private static void migrateLegacyData() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File legacyFile = new File(configDir, LEGACY_FILE);
        
        if (legacyFile.exists()) {
            PiggyAdmin.LOGGER.info("Found legacy history file, migrating to unified JSON store...");
            try (FileReader reader = new FileReader(legacyFile)) {
                // To keep it simple and robust, we'll just rename the old file to .bak and start fresh
                // as the new system is fundamentally different (StructuredEvent vs raw strings).
                legacyFile.renameTo(new File(configDir, LEGACY_FILE + ".bak"));
                PiggyAdmin.LOGGER.info("Legacy history migrated to backup (.bak). New moderation logs started in {}", MODERATION_FILE);
            } catch (Exception e) {
                PiggyAdmin.LOGGER.error("Failed to migrate legacy history", e);
            }
        }
    }

    // --- Static Query Methods (Bridged to JsonHistoryStore) ---

    public static List<HistoryEntry> getGlobalRecent(int count) {
        return moderationStore.getHistory().stream()
            .map(record -> new HistoryEntry(record.timestamp(), record.eventKey(), record.narrative(), record.data()))
            .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
            .limit(count)
            .collect(Collectors.toList());
    }

    public static List<HistoryEntry> getPlayerHistory(String playerName) {
        if (playerName == null) return Collections.emptyList();
        return moderationStore.getHistory().stream()
            .filter(r -> playerName.equalsIgnoreCase((String) r.data().get("playerName")))
            .map(r -> new HistoryEntry(r.timestamp(), r.eventKey(), r.narrative(), r.data()))
            .collect(Collectors.toList());
    }

    public static List<HistoryEntry> findEntriesNear(String worldId, BlockPos pos, double radius, String... eventKeys) {
        double radiusSq = radius * radius;
        Set<String> keySet = Set.of(eventKeys);

        return moderationStore.getHistory().stream()
            .filter(r -> keySet.isEmpty() || keySet.contains(r.eventKey()))
            .filter(r -> worldId.equals(r.data().get("worldId")))
            .filter(r -> {
                Number x = (Number) r.data().get("x");
                Number y = (Number) r.data().get("y");
                Number z = (Number) r.data().get("z");
                if (x == null || y == null || z == null) return false;
                double dx = x.doubleValue() - pos.getX();
                double dy = y.doubleValue() - pos.getY();
                double dz = z.doubleValue() - pos.getZ();
                return (dx*dx + dy*dy + dz*dz) <= radiusSq;
            })
            .map(r -> new HistoryEntry(r.timestamp(), r.eventKey(), r.narrative(), r.data()))
            .limit(50)
            .collect(Collectors.toList());
    }

    /**
     * Compatibility method for legacy Type-based queries.
     */
    public static List<HistoryEntry> findEntriesNear(String worldId, BlockPos pos, double radius, HistoryEntry.Type... types) {
        String[] keys = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            keys[i] = mapLegacyTypeToKey(types[i]);
        }
        return findEntriesNear(worldId, pos, radius, keys);
    }

    public static Set<String> getKnownPlayerNames() {
        return moderationStore.getHistory().stream()
            .map(r -> (String) r.data().get("playerName"))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public static String getSignInfo(String worldId, BlockPos pos) {
        return findEntriesNear(worldId, pos, 0.5, "piggy.admin.telemetry.chat_moderation").stream()
            .filter(e -> e.narrative() != null && e.narrative().contains("[SIGN]"))
            .map(e -> e.playerName() + ": " + e.narrative())
            .findFirst()
            .orElse("No forensic data found for this sign.");
    }
    
    // --- Legacy Compatibility Wrappers (Delegates to Unified Event Dispatcher) ---

    @Deprecated
    public static HistoryEntry logExplosion(is.pig.minecraft.admin.storage.BlameData blame, String tag) {
        dispatchHazard(blame.authorUuid(), blame.authorName(), is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.THREAT, blame.pos(), blame.worldId());
        return null; // Return null as legacy metadata attachment is now discouraged
    }

    @Deprecated
    public static HistoryEntry logExplosion(net.minecraft.server.level.ServerPlayer player, is.pig.minecraft.admin.storage.BlameData blame, String tag) {
        return logExplosion(blame, tag);
    }

    @Deprecated
    public static HistoryEntry logExplosion(String source, String details, String worldId, BlockPos pos) {
        StructuredEventDispatcher.getInstance().dispatch(new is.pig.minecraft.admin.telemetry.ExplosionDetonationEvent(source, pos.getX() + "," + pos.getY() + "," + pos.getZ(), 4.0f, 0, 0));
        return null;
    }

    @Deprecated
    public static void logFire(is.pig.minecraft.admin.storage.BlameData blame) {
        dispatchHazard(blame.authorUuid(), blame.authorName(), is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.ARSON, blame.pos(), blame.worldId());
    }

    @Deprecated
    public static void logFire(net.minecraft.server.level.ServerPlayer player, is.pig.minecraft.admin.storage.BlameData blame) {
        logFire(blame);
    }

    @Deprecated
    public static void logLava(net.minecraft.server.level.ServerPlayer player, is.pig.minecraft.admin.storage.BlameData blame) {
        dispatchHazard(blame.authorUuid(), blame.authorName(), is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.HAZARD, blame.pos(), blame.worldId());
    }

    @Deprecated
    public static void logLava(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        String worldId = player.serverLevel().dimension().location().toString();
        dispatchHazard(player.getUUID(), player.getName().getString(), is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.HAZARD, pos, worldId);
    }

    @Deprecated
    public static void logLavaBurn(java.util.UUID authorUuid, String authorName, String blockName, String worldId, BlockPos pos) {
        dispatchHazard(authorUuid, authorName, is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.HAZARD, pos, worldId);
    }

    @Deprecated
    public static void logBurn(java.util.UUID authorUuid, String authorName, String blockName, String worldId, BlockPos pos, net.minecraft.server.MinecraftServer server) {
        dispatchHazard(authorUuid, authorName, is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType.ARSON, pos, worldId);
    }

    @Deprecated
    public static void logChat(String playerName, java.util.UUID uuid, String message) {
        StructuredEventDispatcher.getInstance().dispatch(new is.pig.minecraft.admin.telemetry.ChatModerationEvent(playerName, message, "Audited", 1.0, "LOGGED", "(unknown)", 0));
    }

    @Deprecated
    public static void logChat(net.minecraft.server.level.ServerPlayer player, String message) {
        logChat(player.getName().getString(), player.getUUID(), message);
    }

    @Deprecated
    public static void logSign(String playerName, java.util.UUID uuid, String text, String worldId, BlockPos pos) {
        logChat(playerName, uuid, "[SIGN] " + text);
    }

    @Deprecated
    public static void logSign(net.minecraft.server.level.ServerPlayer player, String text, String worldId, BlockPos pos) {
        logChat(player, "[SIGN] " + text);
    }
    
    @Deprecated
    public static void logBlock(net.minecraft.server.level.ServerPlayer player, String content, is.pig.minecraft.admin.moderation.ModerationCategory category, String worldId, BlockPos pos) {
       StructuredEventDispatcher.getInstance().dispatch(new is.pig.minecraft.admin.telemetry.ChatModerationEvent(player.getName().getString(), content, category.name(), 1.0, "BLOCKED", String.format("(%d,%d,%d)", pos.getX(), pos.getY(), pos.getZ()), player.serverLevel().getGameTime()));
    }

    private static void dispatchHazard(java.util.UUID uuid, String name, is.pig.minecraft.admin.telemetry.HazardousPlacementEvent.PlacementType type, BlockPos pos, String worldId) {
        String blockPosStr = String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
        // For legacy dispatch, we might not have a ServerPlayer object, so we use dummy/best-effort values for playerPos and tick
        StructuredEventDispatcher.getInstance().dispatch(new is.pig.minecraft.admin.telemetry.HazardousPlacementEvent(
            name, 
            "Legacy Replacement", 
            blockPosStr, 
            worldId, 
            "(unknown)", 
            0, 
            type
        ));
    }

    private static String mapLegacyTypeToKey(HistoryEntry.Type type) {
        if (type == null) return "piggy.admin.unknown";
        return switch (type) {
            case TNT, EXPLOSION -> "piggy.admin.telemetry.explosion_detonated";
            case FIRE, BURN -> "piggy.admin.telemetry.hazardous_placement"; 
            case LAVA -> "piggy.admin.telemetry.hazardous_placement"; 
            case CHAT -> "piggy.admin.telemetry.chat_moderation";
            case SIGN -> "piggy.admin.telemetry.chat_moderation";
            default -> "piggy.admin.misc";
        };
    }

    public static void load() { init(); }
    public static void save() {} 
    
    public static List<HistoryEntry> findExplosionsAffecting(String worldId, BlockPos pos) {
        return findEntriesNear(worldId, pos, 6.0, "piggy.admin.telemetry.explosion_detonated");
    }
}