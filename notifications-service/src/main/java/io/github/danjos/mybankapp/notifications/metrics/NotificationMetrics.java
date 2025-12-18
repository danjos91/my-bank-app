package io.github.danjos.mybankapp.notifications.metrics;

import io.github.danjos.mybankapp.notifications.entity.Notification;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics collector for notification service operations.
 * Tracks notification delivery, failures, and processing times.
 */
@Component
public class NotificationMetrics {

    private final Map<String, Counter> notificationSentByType;
    private final Counter notificationFailedCounter;
    private final Timer notificationDeliveryTimer;
    private final MeterRegistry meterRegistry;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.notificationSentByType = new ConcurrentHashMap<>();

        // Notification failed counter
        this.notificationFailedCounter = Counter.builder("notification.failed")
                .description("Number of failed notification attempts")
                .tag("result", "failed")
                .register(meterRegistry);

        // Notification delivery time
        this.notificationDeliveryTimer = Timer.builder("notification.delivery.time")
                .description("Time taken to process and deliver notifications")
                .register(meterRegistry);
    }

    /**
     * Record a successful notification sent by type.
     */
    public void recordNotificationSent(Notification.NotificationType type) {
        String typeName = type != null ? type.name() : "UNKNOWN";
        
        Counter counter = notificationSentByType.computeIfAbsent(typeName,
            t -> Counter.builder("notification.sent")
                .description("Number of successfully sent notifications")
                .tag("type", t)
                .register(meterRegistry));
        
        counter.increment();
    }

    /**
     * Record a failed notification.
     */
    public void recordNotificationFailed() {
        notificationFailedCounter.increment();
    }

    /**
     * Get the timer for notification delivery.
     */
    public Timer getNotificationDeliveryTimer() {
        return notificationDeliveryTimer;
    }

    /**
     * Record notification delivery with timing.
     */
    public void recordNotificationDelivery(Notification.NotificationType type, Runnable operation) {
        notificationDeliveryTimer.record(() -> {
            operation.run();
            recordNotificationSent(type);
        });
    }
}

