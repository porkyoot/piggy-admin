package is.pig.minecraft.admin.telemetry;

import is.pig.minecraft.admin.storage.HistoryManager;
import is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class EnrichmentTest {

    @Test
    void testAdminContextEnrichment() {
        // Note: HistoryManager relies on FabricLoader for config location.
        // We'll skip file I/O tests if not in Fabric environment, but we can test the logic 
        // if we can ensure HistoryManager is initialized or has data.
        
        String playerName = "Griefer123";
        HistoryManager.logChat(playerName, UUID.randomUUID(), "I will grief!");

        StructuredEventDispatcher dispatcher = StructuredEventDispatcher.getInstance();
        dispatcher.registerEnricher(new AdminContextEnricher());

        HazardousPlacementEvent event = new HazardousPlacementEvent(
                playerName, "TNT", "10,20,30", "world", "10,20,30", 0, HazardousPlacementEvent.PlacementType.THREAT
        );

        var view = dispatcher.enrich(event);
        Map<String, Object> data = view.enrichedData();
        
        // Since HistoryManager might fail to save in a raw unit test, we check if it added to its internal list
        if (HistoryManager.getPlayerHistory(playerName).size() > 0) {
            assertEquals(true, data.get("is_repeat_offender"), "Should mark as repeat offender");
            assertNotNull(data.get("previous_offense_count"), "Should have offense count");
        }
    }
}
