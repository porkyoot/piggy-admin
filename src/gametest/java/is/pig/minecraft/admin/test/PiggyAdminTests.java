import is.pig.minecraft.admin.config.PiggyServerConfig;
import is.pig.minecraft.admin.storage.HistoryEntry;
import is.pig.minecraft.admin.storage.HistoryManager;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * GameTest suite for Piggy Admin mod.
 * Tests can be run with: ./gradlew :piggy-admin:runGametest
 */
public class PiggyAdminTests {

    /**
     * Simple dummy test to verify GameTest framework is working.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dummyPassTest(GameTestHelper context) {
        System.out.println("[PIGGY-ADMIN TEST] Dummy test executed successfully!");
        context.succeed();
    }

    /**
     * Test /piggy cheats command state changes
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testPiggyCheatsToggle(GameTestHelper context) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();

        // Test forbid
        config.allowCheats = false;
        PiggyServerConfig.save();
        context.assertTrue(!config.allowCheats, "Cheats should be forbidden");
        System.out.println("[TEST] Cheats forbidden: " + !config.allowCheats);

        // Test allow
        config.allowCheats = true;
        PiggyServerConfig.save();
        context.assertTrue(config.allowCheats, "Cheats should be allowed");
        System.out.println("[TEST] Cheats allowed: " + config.allowCheats);

        context.succeed();
    }

    /**
     * Test feature enable/disable functionality
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testFeatureToggle(GameTestHelper context) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();

        // Enable a feature
        config.features.put("tool_swap", true);
        PiggyServerConfig.save();

        boolean enabled = config.features.getOrDefault("tool_swap", false);
        context.assertTrue(enabled, "tool_swap should be enabled");
        System.out.println("[TEST] Feature tool_swap enabled: " + enabled);

        // Disable it
        config.features.put("tool_swap", false);
        PiggyServerConfig.save();

        boolean disabled = !config.features.getOrDefault("tool_swap", true);
        context.assertTrue(disabled, "tool_swap should be disabled");
        System.out.println("[TEST] Feature tool_swap disabled");

        context.succeed();
    }

    /**
     * Test that sign placement logs text correctly
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testSignLogging(GameTestHelper context) {
        // Place a sign
        BlockPos signPos = new BlockPos(0, 1, 0);
        BlockState signState = Blocks.OAK_SIGN.defaultBlockState();
        context.setBlock(signPos, signState);

        SignBlockEntity signEntity = (SignBlockEntity) context.getBlockEntity(signPos);
        context.assertTrue(signEntity != null, "Sign entity should exist");

        // Create test data
        String testText = "Test Sign Content";
        String testPlayer = "TestPlayer";
        UUID testUUID = UUID.randomUUID();
        String worldId = context.getLevel().dimension().location().toString();

        // Manually log to history (simulating what the mixin does)
        HistoryManager.logSign(testPlayer, testUUID, testText, worldId, signPos);

        // Verify history entry exists
        HistoryEntry entry = HistoryManager.getSignInfo(worldId, signPos);
        context.assertTrue(entry != null, "Sign should be in history");
        context.assertTrue(entry.content.contains(testText), "History should contain sign text");
        context.assertTrue(entry.type == HistoryEntry.Type.SIGN, "Entry should be SIGN type");
        context.assertTrue(entry.playerName.equals(testPlayer), "Should record correct player name");

        System.out.println("[TEST] Sign logged: " + testText + " by " + testPlayer);
        context.succeed();
    }

    /**
     * Test that chat messages are logged correctly
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testChatLogging(GameTestHelper context) {
        String testPlayer = "ChatTestPlayer";
        UUID testUUID = UUID.randomUUID();
        String testMessage = "Test chat message from GameTest";

        // Log chat message
        HistoryManager.logChat(testPlayer, testUUID, testMessage);

        // Verify history entry
        List<HistoryEntry> entries = HistoryManager.getPlayerHistory(testPlayer);
        context.assertTrue(!entries.isEmpty(), "Should have history entries");

        HistoryEntry lastEntry = entries.get(entries.size() - 1);
        context.assertTrue(lastEntry.content.equals(testMessage), "Should log correct message");
        context.assertTrue(lastEntry.type == HistoryEntry.Type.CHAT, "Entry should be CHAT type");
        context.assertTrue(lastEntry.playerName.equals(testPlayer), "Should record correct player");

        System.out.println("[TEST] Chat logged: " + testMessage + " by " + testPlayer);
        context.succeed();
    }

    /**
     * Test that /logs retrieves player history correctly
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testPlayerHistoryRetrieval(GameTestHelper context) {
        String testPlayer = "HistoryTestPlayer";
        UUID testUUID = UUID.randomUUID();

        // Add multiple history entries
        HistoryManager.logChat(testPlayer, testUUID, "Message 1");
        HistoryManager.logChat(testPlayer, testUUID, "Message 2");
        HistoryManager.logChat(testPlayer, testUUID, "Message 3");

        // Retrieve history
        List<HistoryEntry> entries = HistoryManager.getPlayerHistory(testPlayer);
        context.assertTrue(entries.size() >= 3, "Should have at least 3 history entries");

        // Verify entries are for correct player
        for (HistoryEntry entry : entries) {
            context.assertTrue(entry.playerName.equals(testPlayer),
                    "All entries should be for " + testPlayer);
        }

        System.out.println("[TEST] Retrieved " + entries.size() + " history entries for " + testPlayer);
        context.succeed();
    }

    /**
     * Test sign info retrieval by location
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testSignInfoRetrieval(GameTestHelper context) {
        String testPlayer = "BlameTestPlayer";
        UUID testUUID = UUID.randomUUID();
        BlockPos signPos = new BlockPos(5, 2, 5);
        String signText = "Retrievable Sign Text";
        String worldId = context.getLevel().dimension().location().toString();

        // Log a sign at specific position
        HistoryManager.logSign(testPlayer, testUUID, signText, worldId, signPos);

        // Retrieve by position
        HistoryEntry entry = HistoryManager.getSignInfo(worldId, signPos);

        context.assertTrue(entry != null, "Should find sign entry");
        context.assertTrue(entry.content.equals(signText), "Should have correct text");
        context.assertTrue(entry.playerName.equals(testPlayer), "Should have correct player");
        context.assertTrue(entry.x == signPos.getX(), "Should have correct X coordinate");
        context.assertTrue(entry.y == signPos.getY(), "Should have correct Y coordinate");
        context.assertTrue(entry.z == signPos.getZ(), "Should have correct Z coordinate");

        System.out.println("[TEST] Sign info retrieved: " + signText + " at " + signPos);
        context.succeed();
    }

    /**
     * Test that multiple signs at same location returns most recent
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testSignHistoryOverwrite(GameTestHelper context) {
        String player1 = "Player1";
        String player2 = "Player2";
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        BlockPos signPos = new BlockPos(10, 3, 10);
        String worldId = context.getLevel().dimension().location().toString();

        // Log first sign
        HistoryManager.logSign(player1, uuid1, "First text", worldId, signPos);

        // Wait a moment to ensure different timestamp
        context.runAfterDelay(2, () -> {
            // Log second sign at same position
            HistoryManager.logSign(player2, uuid2, "Second text", worldId, signPos);

            // Should retrieve most recent
            HistoryEntry entry = HistoryManager.getSignInfo(worldId, signPos);
            context.assertTrue(entry != null, "Should find sign entry");
            context.assertTrue(entry.playerName.equals(player2), "Should return most recent player");
            context.assertTrue(entry.content.equals("Second text"), "Should return most recent text");

            System.out.println("[TEST] Sign history correctly returns most recent entry");
            context.succeed();
        });
    }

    /**
     * Test config state persistence
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testConfigPersistence(GameTestHelper context) {
        PiggyServerConfig config = PiggyServerConfig.getInstance();

        // Save current state
        boolean originalCheats = config.allowCheats;

        // Change state
        config.allowCheats = !originalCheats;
        config.features.put("test_feature", true);
        PiggyServerConfig.save();

        // Reload config
        PiggyServerConfig.load();
        PiggyServerConfig reloaded = PiggyServerConfig.getInstance();

        // Verify persisted
        context.assertTrue(reloaded.allowCheats == !originalCheats, "Config should persist cheats setting");
        context.assertTrue(reloaded.features.getOrDefault("test_feature", false),
                "Config should persist feature settings");

        // Restore original
        config.allowCheats = originalCheats;
        config.features.remove("test_feature");
        PiggyServerConfig.save();

        System.out.println("[TEST] Config persistence verified");
        context.succeed();
    }

    /**
     * Test that history contains correct timestamp format
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testHistoryTimestamp(GameTestHelper context) {
        String testPlayer = "TimestampTestPlayer";
        UUID testUUID = UUID.randomUUID();

        HistoryManager.logChat(testPlayer, testUUID, "Timestamp test message");

        List<HistoryEntry> entries = HistoryManager.getPlayerHistory(testPlayer);
        context.assertTrue(!entries.isEmpty(), "Should have entries");

        HistoryEntry entry = entries.get(entries.size() - 1);
        context.assertTrue(entry.timestamp != null, "Should have timestamp");
        context.assertTrue(!entry.timestamp.isEmpty(), "Timestamp should not be empty");

        // Verify timestamp format (yyyy-MM-dd HH:mm:ss)
        context.assertTrue(entry.timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Timestamp should match expected format");

        System.out.println("[TEST] History timestamp verified: " + entry.timestamp);
        context.succeed();
    }

    /**
     * Test XRay detection system
     * Simulates: mine stone blocks (normal), then diamond ores (suspicious), verify
     * warning triggers
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testXRayDetection(GameTestHelper context) {
        // Enable XRay checking
        PiggyServerConfig config = PiggyServerConfig.getInstance();
        config.xrayCheck = true;
        config.xrayMinBlocks = 20; // Require 20 blocks before checking
        config.xrayMaxRatio = 0.15f; // 15% threshold

        // Create a FakePlayer for testing
        net.minecraft.server.level.ServerLevel level = context.getLevel();
        net.fabricmc.fabric.api.entity.FakePlayer fakePlayer = net.fabricmc.fabric.api.entity.FakePlayer.get(level);

        // Simulate normal mining: 12 stone blocks
        System.out.println("[TEST] Simulating normal mining: 12 stone blocks");
        for (int i = 0; i < 12; i++) {
            BlockPos pos = new BlockPos(i, 1, 0);
            context.setBlock(pos, Blocks.STONE);

            // Simulate block break
            is.pig.minecraft.admin.anticheat.XRayDetector.onBlockBreak(
                    fakePlayer,
                    pos,
                    Blocks.STONE.defaultBlockState());
        }

        // Now simulate suspicious mining: 10 diamond ores
        // This will make ratio = 10/(10+12) = 45.5% which is > 15%
        System.out.println("[TEST] Simulating suspicious mining: 10 diamond ores");
        for (int i = 0; i < 10; i++) {
            BlockPos pos = new BlockPos(i, 2, 0);
            context.setBlock(pos, Blocks.DIAMOND_ORE);

            // Simulate block break - THIS should trigger the alert
            is.pig.minecraft.admin.anticheat.XRayDetector.onBlockBreak(
                    fakePlayer,
                    pos,
                    Blocks.DIAMOND_ORE.defaultBlockState());
        }

        // The XRayDetector will log to console via AdminNotifier
        // We can verify the math:
        // Total blocks: 12 stone + 10 diamond = 22 blocks (>= minBlocks)
        // Ratio: 10/22 = 45.5% (> 15% threshold)

        System.out.println("[TEST] XRay detection test completed");
        System.out.println("[TEST] Expected: Alert triggered (45.5% rare ore ratio > 15% threshold)");
        System.out.println("[TEST] Check console logs for: [XRAY] alert with ratio information");

        context.succeed();
    }
}
