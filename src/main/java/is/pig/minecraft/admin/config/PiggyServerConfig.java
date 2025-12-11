package is.pig.minecraft.admin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import is.pig.minecraft.lib.features.CheatFeature;
import is.pig.minecraft.lib.features.CheatFeatureRegistry;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PiggyServerConfig {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("piggy-admin-server.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static PiggyServerConfig INSTANCE;

    public boolean allowCheats = true;
    
    // --- XRay Configuration ---
    public boolean xrayCheck = true;
    public float xrayMaxRatio = 0.15f; // 15% Rare vs (Rare+Common)
    public int xrayMinBlocks = 20;     // Minimum blocks in window before checking
    
    public java.util.Map<String, Boolean> features = new java.util.HashMap<>();

    public static PiggyServerConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, PiggyServerConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                INSTANCE = new PiggyServerConfig();
            }
        } else {
            INSTANCE = new PiggyServerConfig();
        }

        INSTANCE.ensureAllFeatures();
        save();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ensureAllFeatures() {
        for (CheatFeature feature : CheatFeatureRegistry.getAllFeatures()) {
            features.putIfAbsent(feature.id(), feature.defaultEnabled());
        }
    }
}