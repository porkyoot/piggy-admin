package is.pig.minecraft.admin.telemetry;

import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.lib.util.telemetry.LogEnricher;
import java.util.Map;

/**
 * Enriches administrative telemetry with historical offender context.
 */
public class AdminContextEnricher implements LogEnricher {

    @Override
    public void enrich(Map<String, Object> data) {
        Object nameObj = data.get("playerName");
        if (nameObj instanceof String playerName) {
            var history = HistoryManager.getPlayerHistory(playerName);
            if (history != null && !history.isEmpty()) {
                data.put("is_repeat_offender", true);
                data.put("previous_offense_count", history.size());
            }
        }
    }
}
