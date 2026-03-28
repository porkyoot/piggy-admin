package is.pig.minecraft.admin.moderation;

import is.pig.minecraft.admin.config.PiggyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Registry for word lists. Handles source management, fetching, merging, and pattern compilation.
 */
public class WordListRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("WordListRegistry");
    private static final WordListFetcher FETCHER = new WordListFetcher();

    /** All codes supported by LDNOOBW + custom sources */
    public static final List<String> ALL_LANGUAGE_CODES = List.of(
        "ar", "cs", "da", "de", "en", "eo", "es", "fa", "fi", "fil", "fr", "fr-CA", "hi", "hu", "it", "ja", "ko", "nl", "no", "pl", "pt", "ru", "sv", "th", "tr", "zh"
    );

    private static final Map<String, List<WordListSource>> SOURCES_BY_LANG = new HashMap<>();

    static {
        // LDNOOBW Sources
        for (String lang : ALL_LANGUAGE_CODES) {
            String url = "https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/master/" + lang;
            addSource(lang, new WordListSource("LDNOOBW", lang, url));
        }
        // Additional English Sources
        addSource("en", new WordListSource("dsojevic", "en", "https://raw.githubusercontent.com/dsojevic/profanity-list/main/en.txt"));
        addSource("en", new WordListSource("areebbeigh", "en", "https://raw.githubusercontent.com/areebbeigh/profanityfilter/master/profanityfilter/data/badwords.txt"));
    }

    private static void addSource(String lang, WordListSource source) {
        SOURCES_BY_LANG.computeIfAbsent(lang, k -> new ArrayList<>()).add(source);
    }

    /**
     * Initializes the registry by fetching enabled languages and injecting patterns into the checker.
     */
    public static void initialize(PiggyServerConfig config) {
        if (!config.wordListEnabled) {
            LOGGER.info("Word list moderation is disabled in config.");
            RegexModerationChecker.injectWordListPatterns(Collections.emptyMap());
            return;
        }

        refresh();
    }

    /**
     * Refreshes the word lists by re-fetching and re-compiling patterns.
     */
    public static void refresh() {
        PiggyServerConfig config = PiggyServerConfig.getInstance();
        if (!config.wordListEnabled) {
             RegexModerationChecker.injectWordListPatterns(Collections.emptyMap());
             return;
        }

        List<String> enabledLangs = config.wordListLanguages.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();

        if (enabledLangs.isEmpty()) {
            LOGGER.info("No word list languages enabled.");
            RegexModerationChecker.injectWordListPatterns(Collections.emptyMap());
            return;
        }

        LOGGER.info("Refreshing word lists for languages: {}", enabledLangs);

        Map<String, CompletableFuture<List<String>>> languageFutures = new HashMap<>();

        for (String lang : enabledLangs) {
            List<WordListSource> sources = SOURCES_BY_LANG.getOrDefault(lang, Collections.emptyList());
            if (sources.isEmpty()) continue;

            List<CompletableFuture<List<String>>> sourceFutures = sources.stream()
                    .map(FETCHER::fetch)
                    .toList();

            CompletableFuture<List<String>> mergedFuture = CompletableFuture.allOf(sourceFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        Set<String> allWords = new HashSet<>();
                        for (CompletableFuture<List<String>> f : sourceFutures) {
                            allWords.addAll(f.join());
                        }
                        return new ArrayList<>(allWords);
                    });

            languageFutures.put(lang, mergedFuture);
        }

        CompletableFuture.allOf(languageFutures.values().toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    Map<String, Pattern> compiledPatterns = new HashMap<>();
                    for (Map.Entry<String, CompletableFuture<List<String>>> entry : languageFutures.entrySet()) {
                        String lang = entry.getKey();
                        List<String> words = entry.getValue().join();
                        if (!words.isEmpty()) {
                            Pattern pattern = compilePattern(words);
                            if (pattern != null) {
                                compiledPatterns.put(lang, pattern);
                            }
                        }
                    }
                    LOGGER.info("Word list refresh complete. Loaded {} language patterns.", compiledPatterns.size());
                    RegexModerationChecker.injectWordListPatterns(compiledPatterns);
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to refresh word lists: {}", ex.getMessage());
                    return null;
                });
    }

    private static Pattern compilePattern(List<String> words) {
        // Filter out very short words and escape for regex
        List<String> validWords = words.stream()
                .filter(w -> w.length() >= 2)
                .map(Pattern::quote)
                .sorted((a, b) -> b.length() - a.length()) // Longest first to avoid partial matches
                .toList();

        if (validWords.isEmpty()) return null;

        // Build the regex with word boundaries
        // Handle phrases with spaces by only applying word boundaries to start/end
        String regex = "\\b(" + String.join("|", validWords) + ")\\b";
        try {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        } catch (Exception e) {
            LOGGER.error("Failed to compile word list pattern: {}", e.getMessage());
            return null;
        }
    }
}
