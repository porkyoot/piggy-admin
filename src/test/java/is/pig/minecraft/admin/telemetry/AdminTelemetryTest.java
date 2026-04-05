package is.pig.minecraft.admin.telemetry;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class AdminTelemetryTest {

    @Test
    void testHazardousPlacementEventArson() {
        HazardousPlacementEvent event = new HazardousPlacementEvent(
                "Player1", "Fire", "10,20,30", "overworld", "10,21,30", 1234L, HazardousPlacementEvent.PlacementType.ARSON
        );
        
        assertEquals("piggy.admin.telemetry.hazardous_placement", event.getEventKey());
        assertEquals("🔥", event.getCategoryIcon());
        
        Map<String, Object> data = event.getEventData();
        assertEquals("Player1", data.get("playerName"));
        assertEquals("Fire", data.get("targetName"));
        assertEquals("10,20,30", data.get("blockPos"));
        assertEquals("overworld", data.get("dimension"));
        assertEquals("ARSON", data.get("type"));
        
        assertTrue(event.formatted().contains("[10,21,30] [ARSON] Player1 placed Fire at 10,20,30"));
    }

    @Test
    void testChatModerationEvent() {
        ChatModerationEvent event = new ChatModerationEvent(
                "Player2", "I love TNT", "Toxic", 0.95, "BLOCKED", "5,5,5", 5678L
        );
        
        assertEquals("piggy.admin.telemetry.chat_moderation", event.getEventKey());
        
        Map<String, Object> data = event.getEventData();
        assertEquals("Player2", data.get("playerName"));
        assertEquals("I love TNT", data.get("originalMessage"));
        assertEquals("Toxic", data.get("moderationCategory"));
        assertEquals(0.95, data.get("confidenceScore"));
        assertEquals("BLOCKED", data.get("actionTaken"));
        
        assertTrue(event.formatted().contains("[5,5,5] [MODERATION]"));
    }

    @Test
    void testHazardousPlacementEventThreat() {
        HazardousPlacementEvent event = new HazardousPlacementEvent(
                "Player3", "TNT", "100,64,100", "overworld", "100,65,100", 1000L, HazardousPlacementEvent.PlacementType.THREAT
        );
        
        assertEquals("piggy.admin.telemetry.hazardous_placement", event.getEventKey());
        assertEquals("💣", event.getCategoryIcon());
        
        Map<String, Object> data = event.getEventData();
        assertEquals("Player3", data.get("playerName"));
        assertEquals("TNT", data.get("targetName"));
        assertEquals("100,64,100", data.get("blockPos"));
        
        assertTrue(event.formatted().contains("[100,65,100] [THREAT] Player3 placed TNT at 100,64,100"));
    }

    @Test
    void testExplosionDetonationEvent() {
        ExplosionDetonationEvent event = new ExplosionDetonationEvent(
                "minecraft:creeper", "200,64,200", 3.0f, 15, 2000L
        );
        
        assertEquals("piggy.admin.telemetry.explosion_detonated", event.getEventKey());
        assertEquals("💥", event.getCategoryIcon());
        
        Map<String, Object> data = event.getEventData();
        assertEquals("minecraft:creeper", data.get("sourceEntity"));
        assertEquals(3.0f, data.get("explosionRadius"));
        assertEquals(15, data.get("blocksDestroyedCount"));
        
        assertTrue(event.formatted().contains("[200,64,200] [EXPLOSION]"));
    }
}
