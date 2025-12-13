package io.github.danjos.mybankapp.notifications.kafka;

import io.github.danjos.mybankapp.notifications.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Generic notification event that can be published by any service.
 * This event is deserialized from Kafka messages and processed by the consumer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Unique event ID for idempotency checking
     */
    private String eventId;
    
    /**
     * User ID who should receive the notification
     */
    private Long userId;
    
    /**
     * Type of notification
     */
    private Notification.NotificationType notificationType;
    
    /**
     * Notification title
     */
    private String title;
    
    /**
     * Notification message body
     */
    private String message;
    
    /**
     * Source service that generated the event (for tracking)
     */
    private String sourceService;
    
    /**
     * Timestamp when event was created (epoch milliseconds)
     */
    private Long timestamp;
    
    /**
     * Additional metadata as JSON string (optional)
     */
    private String metadata;
}

