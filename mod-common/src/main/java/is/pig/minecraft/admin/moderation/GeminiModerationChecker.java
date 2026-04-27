package is.pig.minecraft.admin.moderation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import is.pig.minecraft.admin.api.IChatIntercept;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Pure Java Gemini moderation checker.
 */
public class GeminiModerationChecker {
    private static final Gson GSON = new Gson();

    public CompletableFuture<Void> check(IChatIntercept intercept, String apiKey, String model, String systemPrompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            boolean blocked = callGeminiDirectly(apiKey, model, systemPrompt, intercept.getMessage());
            if (blocked) {
                intercept.cancel();
            }
        });
    }

    private boolean callGeminiDirectly(String apiKey, String model, String systemPrompt, String message) {
        try {
            JsonObject systemInstruction = new JsonObject();
            parts(systemInstruction, systemPrompt);

            JsonObject contents = new JsonObject();
            parts(contents, message);

            JsonObject root = new JsonObject();
            root.add("system_instruction", systemInstruction);
            com.google.gson.JsonArray contentsArray = new com.google.gson.JsonArray();
            contentsArray.add(contents);
            root.add("contents", contentsArray);

            URL url = new URI("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey).toURL();
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

                    int start = text.indexOf("{");
                    int end = text.lastIndexOf("}");
                    if (start != -1 && end != -1) {
                        String jsonStr = text.substring(start, end + 1);
                        JsonObject moderation = GSON.fromJson(jsonStr, JsonObject.class);
                        String categoryStr = moderation.get("category").getAsString();
                        
                        if (!categoryStr.equalsIgnoreCase("SAFE")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent catch for network/parsing errors
        }
        return false;
    }

    private void parts(JsonObject target, String text) {
        com.google.gson.JsonArray parts = new com.google.gson.JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        target.add("parts", parts);
    }
}
