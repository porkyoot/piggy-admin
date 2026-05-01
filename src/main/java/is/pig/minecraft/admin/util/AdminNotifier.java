package is.pig.minecraft.admin.util;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.RenderPipelineAdapter;
import is.pig.minecraft.admin.ui.AdminHudNotifier;
import is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decoupled Admin Notifier that formats telemetry events and passes them to the rendering pipeline.
 */
public class AdminNotifier {

    /**
     * Registers the AdminNotifier as a listener to the global event dispatcher.
     */
    public static void register() {
        is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance().registerListener(view -> {
            if (view.parent().isNotable() || view.parent().isFailure()) {
                broadcastAdminEvent(view.parent());
            }
        });
    }

    /**
     * Formats a structured event and queues it for the HUD rendering pipeline.
     */
    public static void broadcastAdminEvent(StructuredEvent event) {
        String narrative = PiggyTelemetryFormatter.formatNarrative(event);
        if (narrative == null || narrative.isEmpty()) return;

        // Queue for HUD display (if on client)
        AdminHudNotifier.notify(narrative);

        // Also log to console
        // Note: In a fully decoupled mod, LOGGER would come from the API or a provider
        // But for now we use the local one if available or just stdout
        System.out.println("[Admin Notification] " + narrative);
    }

    /**
     * Decoupled notification method.
     */
    public static void notifyAdmins(Object player, String tag, BlockPos at, String worldId, String content) {
        String message = String.format("[%s] %s at %s (%s)", tag, content, at, worldId);
        AdminHudNotifier.notify(message);
    }

    public static void notifyAdmins(Object player, String tag, BlockPos at, String content) {
        notifyAdmins(player, tag, at, "unknown", content);
    }
}