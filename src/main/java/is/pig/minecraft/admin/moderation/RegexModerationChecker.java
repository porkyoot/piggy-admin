package is.pig.minecraft.admin.moderation;

import is.pig.minecraft.admin.config.PiggyServerConfig;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexModerationChecker implements ModerationChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("RegexModeration");
    private List<CompiledRule> compiledRules = new ArrayList<>();
    private static volatile List<CompiledRule> wordListRules = new ArrayList<>();

    public RegexModerationChecker() {
        reload();
    }

    public void reload() {
        List<CompiledRule> newRules = new ArrayList<>();
        for (PiggyServerConfig.ModerationRule rule : PiggyServerConfig.getInstance().moderationRules) {
            if (rule.enabled && rule.regex != null && !rule.regex.isEmpty()) {
                try {
                    newRules.add(new CompiledRule(rule.category, Pattern.compile(rule.regex)));
                } catch (PatternSyntaxException e) {
                    // Log error but continue
                    LOGGER.warn("Invalid regex in moderation rule: {}", rule.regex);
                }
            }
        }
        this.compiledRules = newRules;
    }

    /**
     * Injects compiled patterns from external word-list databases.
     */
    public static void injectWordListPatterns(java.util.Map<String, Pattern> patternsByLang) {
        List<CompiledRule> newRules = new ArrayList<>();
        for (var entry : patternsByLang.entrySet()) {
            newRules.add(new CompiledRule(ModerationCategory.SWEARS, entry.getValue()));
        }
        wordListRules = newRules;
        LOGGER.info("Injected {} word-list patterns.", newRules.size());
    }

    @Override
    public CompletableFuture<ModerationResult> check(ServerPlayer player, String message) {
        return CompletableFuture.supplyAsync(() -> {
            for (CompiledRule rule : compiledRules) {
                if (rule.pattern.matcher(message).find()) {
                    LOGGER.debug("Regex match found for category {}: {}", rule.category, message);
                    return ModerationResult.blocked(rule.category, "Regex match: " + rule.category, 1.0);
                }
            }
            for (CompiledRule rule : wordListRules) {
                 if (rule.pattern.matcher(message).find()) {
                     LOGGER.debug("Word-list match found: {}", message);
                     return ModerationResult.blocked(rule.category, "Word-list match", 1.0);
                 }
            }
            return ModerationResult.SAFE;
        });
    }


    @Override
    public String getName() {
        return "Regex";
    }

    private record CompiledRule(ModerationCategory category, Pattern pattern) {}
}
