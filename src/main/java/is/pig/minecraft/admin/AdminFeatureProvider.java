package is.pig.minecraft.admin;

import is.pig.minecraft.api.spi.FeatureProvider;
import is.pig.minecraft.api.spi.PiggyFeature;
import is.pig.minecraft.admin.anticheat.HybridXRayDetector;
import is.pig.minecraft.admin.anticheat.XRayDetector;
import is.pig.minecraft.admin.moderation.GeminiModerationChecker;
import is.pig.minecraft.admin.moderation.RegexModerationChecker;

import java.util.List;

/**
 * Service Provider for Piggy Admin features.
 * Returns instances of moderation tools and anti-cheat modules.
 */
public class AdminFeatureProvider implements FeatureProvider {

    private final XRayDetector xrayDetector = new XRayDetector();
    private final HybridXRayDetector hybridXRayDetector = new HybridXRayDetector();
    private final GeminiModerationChecker geminiModerationChecker = new GeminiModerationChecker();
    private final RegexModerationChecker regexModerationChecker = new RegexModerationChecker();

    @Override
    public List<PiggyFeature> getFeatures() {
        return List.of(
            new PiggyFeature("piggy-admin:xray-detector", "X-Ray Detector", true, "Statistical analysis for identifying X-Ray users.", xrayDetector),
            new PiggyFeature("piggy-admin:hybrid-xray-detector", "Hybrid X-Ray Detector", true, "Advanced heuristic and pattern-based X-Ray detection.", hybridXRayDetector),
            new PiggyFeature("piggy-admin:gemini-moderation", "Gemini Moderation", true, "AI-powered chat and behavior moderation using Google Gemini.", geminiModerationChecker),
            new PiggyFeature("piggy-admin:regex-moderation", "Regex Moderation", true, "Pattern-based content filtering.", regexModerationChecker)
        );
    }

    @Override
    public String getFeatureId() {
        return "piggy-admin";
    }

    @Override
    public void onFeatureToggle(String featureId, boolean state) {
        // Handle dynamic enabling/disabling if necessary
    }
}
