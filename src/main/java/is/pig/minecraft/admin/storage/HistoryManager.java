package is.pig.minecraft.admin.storage;

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

    public static List<HistoryEntry> getPlayerHistory(String playerName) {
        return history.stream()
                .filter(e -> e.playerName.equalsIgnoreCase(playerName))
                .collect(Collectors.toList());
    }

    public static HistoryEntry getSignInfo(String worldId, BlockPos pos) {
        List<HistoryEntry> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        
        for (HistoryEntry entry : reversed) {
            if (entry.type == HistoryEntry.Type.SIGN &&
                entry.worldId.equals(worldId) &&
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