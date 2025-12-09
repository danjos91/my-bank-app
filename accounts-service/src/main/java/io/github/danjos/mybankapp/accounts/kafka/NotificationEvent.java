package io.github.danjos.mybankapp.accounts.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Notification event published to Kafka.
 * Consumed by notifications-service to create notification records.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String eventId;
    private Long userId;
    private String notificationType;
    private String title;
    private String message;
    private String sourceService;
    private Long timestamp;
    private String metadata;
}

