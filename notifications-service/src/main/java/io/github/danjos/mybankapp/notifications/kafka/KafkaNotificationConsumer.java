package io.github.danjos.mybankapp.notifications.kafka;

import io.github.danjos.mybankapp.notifications.dto.CreateNotificationDTO;
import io.github.danjos.mybankapp.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Kafka consumer for notification events.
 * Listens to multiple topics and creates notification records in the database.
 * Implements "at least once" delivery semantics with manual acknowledgment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Validated
public class KafkaNotificationConsumer {
    
    private final NotificationService notificationService;
    
    /**
     * Consumes notification events from all notification topics.
     * Uses manual acknowledgment to ensure "at least once" delivery.
     * 
     * Topics:
     * - account-created: New account registrations
     * - account-updated: Account modifications
     * - deposit-completed: Successful deposits
     * - withdrawal-completed: Successful withdrawals
     * - transfer-initiated: Transfer started
     * - transfer-completed: Transfer succeeded
     * - transfer-failed: Transfer failed
     * - notification-event: Generic notifications
     */
    @KafkaListener(
        topics = {
            "account-created",
            "account-updated",
            "deposit-completed",
            "withdrawal-completed",
            "transfer-initiated",
            "transfer-completed",
            "transfer-failed",
            "notification-event"
        },
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotificationEvent(
            @Payload NotificationEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received notification event from topic '{}' [partition={}, offset={}]: eventId={}, userId={}, type={}",
                    topic, partition, offset, event.getEventId(), event.getUserId(), event.getNotificationType());
            
            // Validate event
            if (event.getUserId() == null) {
                log.error("Invalid event: userId is null. EventId={}", event.getEventId());
                // Acknowledge to prevent reprocessing (poison message)
                acknowledgment.acknowledge();
                return;
            }
            
            if (event.getNotificationType() == null) {
                log.error("Invalid event: notificationType is null. EventId={}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }
            
            // Check for idempotency using eventId
            // In a production system, you would check if this eventId was already processed
            // For now, we'll rely on database constraints and timestamps
            
            // Convert event to DTO
            CreateNotificationDTO notificationDTO = CreateNotificationDTO.builder()
                    .userId(event.getUserId())
                    .notificationType(event.getNotificationType())
                    .title(event.getTitle() != null ? event.getTitle() : getDefaultTitle(event.getNotificationType()))
                    .message(event.getMessage() != null ? event.getMessage() : getDefaultMessage(event.getNotificationType()))
                    .build();
            
            // Create notification in database
            notificationService.createNotification(notificationDTO);
            
            log.info("Successfully processed notification event: eventId={}, userId={}, type={}",
                    event.getEventId(), event.getUserId(), event.getNotificationType());
            
            // Manually acknowledge after successful processing
            // This ensures "at least once" delivery
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing notification event from topic '{}': eventId={}, error={}",
                    topic, event.getEventId(), e.getMessage(), e);
            
            // Don't acknowledge - message will be redelivered
            // In production, consider:
            // 1. Retry with exponential backoff
            // 2. Send to Dead Letter Queue after max retries
            // 3. Alert monitoring system
            throw e;
        }
    }
    
    /**
     * Provides default titles based on notification type.
     */
    private String getDefaultTitle(io.github.danjos.mybankapp.notifications.entity.Notification.NotificationType type) {
        return switch (type) {
            case ACCOUNT_CREATED -> "Welcome to MyBank!";
            case ACCOUNT_UPDATED -> "Account Updated";
            case DEPOSIT_SUCCESS -> "Deposit Successful";
            case WITHDRAWAL_SUCCESS -> "Withdrawal Successful";
            case TRANSFER_SENT -> "Transfer Sent";
            case TRANSFER_RECEIVED -> "Transfer Received";
            case TRANSFER_FAILED -> "Transfer Failed";
            case BALANCE_LOW -> "Low Balance Alert";
            case SUSPICIOUS_ACTIVITY -> "Security Alert";
            default -> "Notification";
        };
    }
    
    /**
     * Provides default messages based on notification type.
     */
    private String getDefaultMessage(io.github.danjos.mybankapp.notifications.entity.Notification.NotificationType type) {
        return switch (type) {
            case ACCOUNT_CREATED -> "Your account has been successfully created.";
            case ACCOUNT_UPDATED -> "Your account information has been updated.";
            case DEPOSIT_SUCCESS -> "Your deposit has been processed successfully.";
            case WITHDRAWAL_SUCCESS -> "Your withdrawal has been processed successfully.";
            case TRANSFER_SENT -> "Your transfer has been sent.";
            case TRANSFER_RECEIVED -> "You have received a transfer.";
            case TRANSFER_FAILED -> "Your transfer could not be processed.";
            case BALANCE_LOW -> "Your account balance is low.";
            case SUSPICIOUS_ACTIVITY -> "Suspicious activity detected on your account.";
            default -> "You have a new notification.";
        };
    }
}

