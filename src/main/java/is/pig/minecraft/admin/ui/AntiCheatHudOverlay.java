package is.pig.minecraft.admin.ui;

import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.RenderPipelineAdapter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * HUD overlay for anti-cheat feedback, using decoupled rendering SPI.
 */
public class AntiCheatHudOverlay {
    private static final String BLOCKED_ICON = "piggy:textures/gui/icons/cheating_cancel.png";
    private static final AtomicLong EXPIRATION_TIME = new AtomicLong(0);
    private static final int DISPLAY_DURATION_MS = 1500;

    public static void triggerBlockedIcon() {
        EXPIRATION_TIME.set(System.currentTimeMillis() + DISPLAY_DURATION_MS);
    }

    public static void render(Object context) {
        if (System.currentTimeMillis() < EXPIRATION_TIME.get()) {
            RenderPipelineAdapter renderer = PiggyServiceRegistry.getRenderPipelineAdapters().stream().findFirst().orElse(null);
            if (renderer == null) return;

            int sw = renderer.getScreenWidth(context);
            int sh = renderer.getScreenHeight(context);
            
            // Render the blocked icon below the crosshair
            renderer.drawTexture(context, BLOCKED_ICON, (sw / 2) - 4, (sh / 2) + 10, 8, 8);
        }
    }
}
