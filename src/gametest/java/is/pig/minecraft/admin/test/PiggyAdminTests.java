package is.pig.minecraft.admin.test;

import is.pig.minecraft.admin.config.PiggyServerConfig;
import is.pig.minecraft.admin.storage.HistoryEntry;
import is.pig.minecraft.admin.storage.HistoryManager;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import java.util.List;
import java.util.UUID;

/**
 * GameTest suite for Piggy Admin mod.
 * Tests updated for Unified Telemetry Architecture.
 */
public class PiggyAdminTests {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dummyPassTest(GameTestHelper context) {
        context.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testSignLogging(GameTestHelper context) {
        BlockPos signPos = new BlockPos(0, 1, 0);
        String testText = "Test Sign Content";
        String testPlayer = "TestPlayer";
        UUID testUUID = UUID.randomUUID();
        String worldId = context.getLevel().dimension().location().toString();

        // Log sign (using legacy wrapper)
        HistoryManager.logSign(testPlayer, testUUID, testText, worldId, signPos);

        // Verify history via search
        List<HistoryEntry> entries = HistoryManager.findEntriesNear(worldId, signPos, 0.5, "piggy.admin.telemetry.chat_moderation");
        context.assertTrue(!entries.isEmpty(), "Sign should be in history");
        
        HistoryEntry entry = entries.get(0);
        context.assertTrue(entry.narrative().contains(testText), "History should contain sign text");
        context.assertTrue(entry.eventKey().contains("chat_moderation"), "Entry should be chat_moderation key");

        context.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testChatLogging(GameTestHelper context) {
        String testPlayer = "ChatTestPlayer";
        UUID testUUID = UUID.randomUUID();
        String testMessage = "Test chat message";

        HistoryManager.logChat(testPlayer, testUUID, testMessage);

        List<HistoryEntry> entries = HistoryManager.getPlayerHistory(testPlayer);
        context.assertTrue(!entries.isEmpty(), "Should have history entries");

        HistoryEntry lastEntry = entries.get(entries.size() - 1);
        context.assertTrue(lastEntry.narrative().contains(testMessage), "Should log correct message");
        context.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testHistoryTimestamp(GameTestHelper context) {
        String testPlayer = "TimestampPlayer";
        HistoryManager.logChat(testPlayer, UUID.randomUUID(), "Msg");

        List<HistoryEntry> entries = HistoryManager.getPlayerHistory(testPlayer);
        context.assertTrue(!entries.isEmpty(), "Should have entries");

        HistoryEntry entry = entries.get(0);
        context.assertTrue(entry.timestamp() != null && !entry.timestamp().isEmpty(), "Should have valid timestamp string");
        context.succeed();
    }
}
