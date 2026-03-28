package is.pig.minecraft.admin.moderation;

import is.pig.minecraft.admin.config.PiggyServerConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handles fetching word lists from online sources with local disk caching.
 */
public class WordListFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger("WordListFetcher");
    private static final Path CACHE_DIR = FabricLoader.getInstance().getConfigDir().resolve("piggy-wordlists");
    private final HttpClient httpClient;

    public WordListFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Fetches a word list from a source, using the cache if possible.
     */
    public CompletableFuture<List<String>> fetch(WordListSource source) {
        return CompletableFuture.supplyAsync(() -> {
            Path cacheFile = CACHE_DIR.resolve(source.language() + "-" + source.name().toLowerCase().replace(" ", "-") + ".txt");

            if (isCacheValid(cacheFile)) {
                try {
                    return Files.readAllLines(cacheFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOGGER.warn("Failed to read word list cache for {}: {}", source.name(), e.getMessage());
                }
            }

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(source.url()))
                        .timeout(Duration.ofSeconds(PiggyServerConfig.getInstance().wordListFetchTimeoutSeconds))
                        .header("User-Agent", "PiggyAdmin (Minecraft Mod)")
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String content = response.body();
                    List<String> lines = parseContent(content);
                    saveToCache(cacheFile, lines);
                    return lines;
                } else {
                    LOGGER.warn("Failed to fetch word list from {} (HTTP {})", source.name(), response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.warn("Error fetching word list from {}: {}", source.name(), e.getMessage());
            }

            // Fallback to cache even if stale if network fails
            if (Files.exists(cacheFile)) {
                try {
                    return Files.readAllLines(cacheFile, StandardCharsets.UTF_8);
                } catch (IOException ignored) {}
            }

            return Collections.emptyList();
        });
    }

    private List<String> parseContent(String content) {
        List<String> lines = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private boolean isCacheValid(Path path) {
        if (!Files.exists(path)) return false;
        try {
            Instant lastModified = Files.getLastModifiedTime(path).toInstant();
            Instant now = Instant.now();
            long daysOld = Duration.between(lastModified, now).toDays();
            return daysOld < PiggyServerConfig.getInstance().wordListCacheDays;
        } catch (IOException e) {
            return false;
        }
    }

    private void saveToCache(Path path, List<String> lines) {
        try {
            Files.createDirectories(CACHE_DIR);
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Failed to save word list cache: {}", e.getMessage());
        }
    }
}
