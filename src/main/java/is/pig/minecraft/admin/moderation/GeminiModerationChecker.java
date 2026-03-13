package is.pig.minecraft.admin.moderation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import is.pig.minecraft.admin.config.PiggyServerConfig;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class GeminiModerationChecker implements ModerationChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("GeminiModerationChecker");
    private static final Gson GSON = new Gson();
    
    // Caching for seen messages (Message -> Result)
    private static final Map<String, ModerationResult> CACHE = new ConcurrentHashMap<>();
    
    // Simple rate limiting (1 request per second for the free tier)
    private static final AtomicLong LAST_REQUEST_TIME = new AtomicLong(0);
    private static final long MIN_INTERVAL_MS = 1000;

    @Override
    public CompletableFuture<ModerationResult> check(ServerPlayer player, String message) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();
        String apiKey = config.geminiApiKey;
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return ModerationResult.safeFuture();
        }

        // 1. Check Cache
        if (CACHE.containsKey(message)) {
            return CompletableFuture.completedFuture(CACHE.get(message));
        }

        return CompletableFuture.supplyAsync(() -> {
            // 2. Simple Rate Limiting
            long now = System.currentTimeMillis();
            long last = LAST_REQUEST_TIME.get();
            if (now - last < MIN_INTERVAL_MS) {
                return ModerationResult.SAFE; 
            }
            LAST_REQUEST_TIME.set(now);

            ModerationResult result = callGeminiDirectly(config, message, player);
            
            // update cache
            if (CACHE.size() > 1000) CACHE.clear();
            CACHE.put(message, result);
            
            return result;
        });
    }

    private ModerationResult callGeminiDirectly(PiggyServerConfig config, String message, ServerPlayer player) {
        try {
            String dynamicSystemPrompt = buildSystemPrompt();
            
            // Build Payload
            JsonObject systemInstruction = new JsonObject();
            parts(systemInstruction, dynamicSystemPrompt);

            JsonObject contents = new JsonObject();
            parts(contents, message);

            JsonObject root = new JsonObject();
            root.add("system_instruction", systemInstruction);
            com.google.gson.JsonArray contentsArray = new com.google.gson.JsonArray();
            contentsArray.add(contents);
            root.add("contents", contentsArray);

            URL url = new URI("https://generativelanguage.googleapis.com/v1beta/models/" + config.geminiModel + ":generateContent?key=" + config.geminiApiKey).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(GSON.toJson(root).getBytes("utf-8"));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A")) {
                    String response = s.hasNext() ? s.next() : "";
                    JsonObject jsonResponse = GSON.fromJson(response, JsonObject.class);
                    String text = jsonResponse.getAsJsonArray("candidates")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("content")
                            .getAsJsonArray("parts")
                            .get(0).getAsJsonObject()
                            .get("text").getAsString();

                    // Extract JSON
                    int start = text.indexOf("{");
                    int end = text.lastIndexOf("}");
                    if (start != -1 && end != -1) {
                        String jsonStr = text.substring(start, end + 1);
                        JsonObject moderation = GSON.fromJson(jsonStr, JsonObject.class);
                        String categoryStr = moderation.get("category").getAsString();
                        ModerationCategory category = ModerationCategory.fromString(categoryStr);
                        
                        if (category != ModerationCategory.SAFE) {
                            LOGGER.info("Gemini flagged message from {} as {}: {}", player.getName().getString(), category, message);
                            return ModerationResult.blocked(category, "Gemini AI: " + category.getDisplayName());
                        }
                    }
                }
            } else {
                try (Scanner s = new Scanner(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()).useDelimiter("\\A")) {
                    String error = s.hasNext() ? s.next() : "";
                    LOGGER.error("Gemini API Error {}: {}", responseCode, error);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Gemini call failed", e);
        }
        return ModerationResult.SAFE;
    }

    private void parts(JsonObject target, String text) {
        com.google.gson.JsonArray parts = new com.google.gson.JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        target.add("parts", parts);
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder(PiggyServerConfig.getInstance().geminiSystemPrompt);
        sb.append("\n\nReply ONLY with a JSON object:\n");
        sb.append("{\"category\": \"CATEGORY_NAME\", \"reason\": \"brief explanation\"}\n\n");
        sb.append("Categories:\n");
        for (ModerationCategory cat : ModerationCategory.values()) {
            sb.append("- ").append(cat.name()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String getName() {
        return "Gemini";
    }
}
