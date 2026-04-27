package is.pig.minecraft.admin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import is.pig.minecraft.admin.moderation.ModerationCategory;
import net.fabricmc.loader.api.FabricLoader;
import is.pig.minecraft.lib.features.CheatFeature;
import is.pig.minecraft.lib.features.CheatFeatureRegistry;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import is.pig.minecraft.lib.config.PiggyConfigManager;

public class PiggyServerConfig extends PiggyConfigManager<PiggyServerConfig> {
    private static final java.io.File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("piggy-admin-server.json")
            .toFile();

    private static PiggyServerConfig INSTANCE;

    public boolean allowCheats = true;
    
    // --- XRay Configuration ---
    public boolean xrayCheck = true;
    public float xrayMaxRatio = 0.15f; // 15% Rare vs (Rare+Common)
    public int xrayMinBlocks = 20;     // Minimum blocks in window before checking
    
    public boolean xrayHybridCheck = true;
    public double xrayHybridThreshold = 0.05;
    
    public java.util.Map<String, Boolean> features = new java.util.HashMap<>();

    // --- Moderation Configuration ---
    public boolean moderationEnabled = true;
    public java.util.List<ModerationRule> moderationRules = new java.util.ArrayList<>();

    // --- Word List Configuration ---
    /** Master switch: when false, no lists are downloaded and word-list rules are cleared. */
    public boolean wordListEnabled = true;
    /**
     * ISO-639-1 language code → enabled. Only enabled languages are fetched.
     * All 26 LDNOOBW languages are pre-populated; only "en" defaults to true.
     */
    public java.util.Map<String, Boolean> wordListLanguages = new java.util.LinkedHashMap<>();
    /** How many days a cached word-list file is considered fresh before re-downloading. */
    public int wordListCacheDays = 7;
    /** Per-source HTTP fetch timeout in seconds. */
    public int wordListFetchTimeoutSeconds = 15;
    
    // --- Gemini LLM Configuration ---
    public String geminiApiKey = "YOUR_GEMINI_API_KEY_HERE";
    public String geminiSystemPrompt = "You are a Minecraft server moderator. Analyze chat messages for toxicity, hate speech, harassment, sexual content, or dangerous content.";
    public String geminiModel = "gemini-2.5-flash";

    private PiggyServerConfig() {
        super("piggy-admin-server.json", PiggyServerConfig.class, "PiggyAdmin-Config", createCustomGsonBuilder());
    }

    private static GsonBuilder createCustomGsonBuilder() {
        return new GsonBuilder()
                .registerTypeAdapter(ModerationCategory.class, new com.google.gson.TypeAdapter<ModerationCategory>() {
                    @Override
                    public void write(com.google.gson.stream.JsonWriter out, ModerationCategory value) throws java.io.IOException {
                        if (value == null) {
                            out.nullValue();
                        } else {
                            out.value(value.name());
                        }
                    }

                    @Override
                    public ModerationCategory read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
                        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                            in.nextNull();
                            return null;
                        }
                        return ModerationCategory.fromString(in.nextString());
                    }
                });
    }

    @Override
    protected PiggyServerConfig getConfigInstance() {
        return INSTANCE;
    }

    @Override
    protected void setConfigInstance(PiggyServerConfig instance) {
        INSTANCE = instance;
    }

    public static class ModerationRule {
        public ModerationCategory category; // Enum category
        public String language; // e.g., en, fr, all
        public String regex;
        public boolean enabled = true;

        public ModerationRule() {}

        public ModerationRule(ModerationCategory category, String language, String regex) {
            this.category = category;
            this.language = language;
            this.regex = regex;
        }
    }

    public static PiggyServerConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PiggyServerConfig();
            INSTANCE.load();
        }
        return INSTANCE;
    }

    public static void load() {
        getInstance().load();
        
        INSTANCE.ensureAllFeatures();
        INSTANCE.ensureWordListLanguages();
        INSTANCE.moderationRules.removeIf(rule -> rule == null || rule.category == null);
        INSTANCE.ensureDefaultModerationRules();
        save();
    }

    public static void save() {
        getInstance().save();
    }

    private void ensureAllFeatures() {
        for (CheatFeature feature : CheatFeatureRegistry.getAllFeatures()) {
            features.putIfAbsent(feature.id(), feature.defaultEnabled());
        }
    }

    private void ensureDefaultModerationRules() {
        if (moderationRules.isEmpty()) {
            LoggerFactory.getLogger("PiggyAdmin-Config").info("Adding default moderation rules...");
            moderationRules.add(new ModerationRule(ModerationCategory.SWEARS, "all", "(?i)\\b(fuck|shit|asshole)\\b"));
            moderationRules.add(new ModerationRule(ModerationCategory.DOX, "all", "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")); // IP regex
        }
    }

    private void ensureWordListLanguages() {
        // All language codes supported by LDNOOBW, plus the two extra English sources
        for (String lang : is.pig.minecraft.admin.moderation.WordListRegistry.ALL_LANGUAGE_CODES) {
            wordListLanguages.putIfAbsent(lang, "en".equals(lang));
        }
    }
}