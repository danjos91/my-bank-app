package io.github.danjos.mybankapp.accounts.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for publishing notification events from accounts-service.
 * Replaces REST-based NotificationsClient for asynchronous notification delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationProducer {
    
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private static final String SOURCE_SERVICE = "accounts-service";
    
    /**
     * Publishes a notification event to Kafka.
     * 
     * @param topic Kafka topic name
     * @param userId User ID (used as message key for partitioning)
     * @param notificationType Type of notification
     * @param title Notification title
     * @param message Notification message
     */
    public void publishNotificationEvent(String topic, Long userId, String notificationType, 
                                        String title, String message) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .userId(userId)
                    .notificationType(notificationType)
                    .title(title)
                    .message(message)
                    .sourceService(SOURCE_SERVICE)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            // Use userId as key for partition assignment (all events for same user go to same partition)
            String key = userId != null ? userId.toString() : UUID.randomUUID().toString();
            
            CompletableFuture<SendResult<String, NotificationEvent>> future = 
                    kafkaTemplate.send(topic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully sent notification event to topic '{}': eventId={}, userId={}, type={}, partition={}, offset={}",
                            topic, event.getEventId(), userId, notificationType,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send notification event to topic '{}': eventId={}, userId={}, type={}, error={}",
                            topic, event.getEventId(), userId, notificationType, ex.getMessage(), ex);
                }
            });
            
        } catch (Exception e) {
            log.error("Error creating notification event: userId={}, type={}, error={}",
                    userId, notificationType, e.getMessage(), e);
        }
    }
    
    /**
     * Publishes account created event.
     */
    public void publishAccountCreatedEvent(Long userId, String accountNumber, String currency) {
        publishNotificationEvent(
                "account-created",
                userId,
                "ACCOUNT_CREATED",
                "Welcome to MyBank!",
                String.format("Your %s account (%s) has been successfully created.", currency, accountNumber)
        );
    }
    
    /**
     * Publishes account updated event.
     */
    public void publishAccountUpdatedEvent(Long userId, String accountNumber) {
        publishNotificationEvent(
                "account-updated",
                userId,
                "ACCOUNT_UPDATED",
                "Account Updated",
                String.format("Your account %s has been updated.", accountNumber)
        );
    }
}

