package is.pig.minecraft.admin.moderation;

import is.pig.minecraft.admin.api.IChatIntercept;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Pure Java regex moderation checker.
 */
public class RegexModerationChecker {

    public CompletableFuture<Void> check(IChatIntercept intercept, List<Rule> rules) {
        return CompletableFuture.runAsync(() -> {
            String message = intercept.getMessage();
            for (Rule rule : rules) {
                if (rule.enabled() && rule.regex() != null && !rule.regex().isEmpty()) {
                    try {
                        if (Pattern.compile(rule.regex()).matcher(message).find()) {
                            intercept.cancel();
                            return;
                        }
                    } catch (Exception e) {
                        // Ignore invalid regex
                    }
                }
            }
        });
    }

    public record Rule(String category, String regex, boolean enabled) {}
}
