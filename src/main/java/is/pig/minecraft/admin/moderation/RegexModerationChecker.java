package is.pig.minecraft.admin.moderation;

import is.pig.minecraft.admin.config.PiggyServerConfig;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.List;
import java.util.ArrayList;

public class RegexModerationChecker implements ModerationChecker {
    private List<CompiledRule> compiledRules = new ArrayList<>();

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
                    System.err.println("Invalid regex in moderation rule: " + rule.regex);
                }
            }
        }
        this.compiledRules = newRules;
    }

    @Override
    public CompletableFuture<ModerationResult> check(ServerPlayer player, String message) {
        return CompletableFuture.supplyAsync(() -> {
            for (CompiledRule rule : compiledRules) {
                if (rule.pattern.matcher(message).find()) {
                    System.out.println("[RegexModeration] Match found for category " + rule.category + ": " + message);
                    return ModerationResult.blocked(rule.category, "Regex match: " + rule.category);
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
