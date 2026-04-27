package is.pig.minecraft.admin.moderation;

import is.pig.minecraft.admin.api.IChatIntercept;

import java.util.List;

/**
 * Orchestrates chat moderation checks.
 * Pure Java, zero Minecraft dependencies.
 */
public class ModerationEngine {
    private final GeminiModerationChecker geminiChecker = new GeminiModerationChecker();
    private final RegexModerationChecker regexChecker = new RegexModerationChecker();

    public void processMessage(IChatIntercept intercept, String apiKey, String model, String systemPrompt, boolean useGemini, List<RegexModerationChecker.Rule> rules) {
        regexChecker.check(intercept, rules).thenRun(() -> {
            if (!intercept.isCancelled() && useGemini) {
                geminiChecker.check(intercept, apiKey, model, systemPrompt);
            }
        });
    }
}
