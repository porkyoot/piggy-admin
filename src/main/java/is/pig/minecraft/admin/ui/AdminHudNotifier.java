package is.pig.minecraft.admin.ui;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.RenderPipelineAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side notifier that renders admin alerts on the HUD using the RenderPipelineAdapter.
 */
public class AdminHudNotifier {
    private static final List<QueuedNotification> activeNotifications = new ArrayList<>();
    private static final int DISPLAY_TIME_MS = 5000;

    private record QueuedNotification(String message, long startTime) {}

    public static void notify(String message) {
        activeNotifications.add(new QueuedNotification(message, System.currentTimeMillis()));
    }

    public static void render(Object context) {
        RenderPipelineAdapter renderer = PiggyServiceRegistry.getRenderPipelineAdapters().stream().findFirst().orElse(null);
        if (renderer == null) return;
        
        long now = System.currentTimeMillis();

        Iterator<QueuedNotification> it = activeNotifications.iterator();
        int y = 10;
        while (it.hasNext()) {
            QueuedNotification notification = it.next();
            if (now - notification.startTime > DISPLAY_TIME_MS) {
                it.remove();
                continue;
            }

            // Draw a semi-transparent background box
            renderer.fillRect(context, 10, y, 200, 15, 0x88000000);
            // Draw the alert text
            renderer.renderText(context, "§c[Admin Alert] §f" + notification.message, 15, (float) y + 3, 0xFFFFFFFF);
            
            y += 20;
        }
    }
}
