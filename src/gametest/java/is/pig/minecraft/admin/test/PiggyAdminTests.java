package is.pig.minecraft.admin.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * GameTest suite for Piggy Admin mod.
 * Tests can be run in-game using /test run or /test runall commands.
 */
public class PiggyAdminTests {

    /**
     * Simple dummy test to verify GameTest framework is working.
     * This test will always pass and logs a message to confirm registration.
     * 
     * Run with: /test run piggy-admin:dummypasstest
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dummyPassTest(GameTestHelper context) {
        // Log confirmation that test executed
        System.out.println("[PIGGY-ADMIN TEST] Dummy test executed successfully!");
        context.succeed();
    }
}
