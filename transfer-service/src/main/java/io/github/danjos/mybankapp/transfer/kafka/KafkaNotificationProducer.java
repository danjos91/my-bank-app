package io.github.danjos.mybankapp.transfer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for publishing notification events from transfer-service.
 * Replaces REST-based NotificationsClient for asynchronous notification delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationProducer {
    
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private static final String SOURCE_SERVICE = "transfer-service";
    
    /**
     * Publishes a notification event to Kafka.
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
            
            String key = userId != null ? userId.toString() : UUID.randomUUID().toString();
            
            CompletableFuture<SendResult<String, NotificationEvent>> future = 
                    kafkaTemplate.send(topic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully sent notification event to topic '{}': eventId={}, userId={}, type={}",
                            topic, event.getEventId(), userId, notificationType);
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
     * Publishes transfer initiated event.
     */
    public void publishTransferInitiatedEvent(Long senderUserId, BigDecimal amount, String currency) {
        publishNotificationEvent(
                "transfer-initiated",
                senderUserId,
                "TRANSFER_SENT",
                "Transfer Initiated",
                String.format("Your transfer of %s %s has been initiated.", amount, currency)
        );
    }
    
    /**
     * Publishes transfer completed event for sender.
     */
    public void publishTransferCompletedEvent(Long senderUserId, BigDecimal amount, String currency, String recipientAccount) {
        publishNotificationEvent(
                "transfer-completed",
                senderUserId,
                "TRANSFER_SENT",
                "Transfer Completed",
                String.format("Your transfer of %s %s to account %s was successful.", amount, currency, recipientAccount)
        );
    }
    
    /**
     * Publishes transfer received event for recipient.
     */
    public void publishTransferReceivedEvent(Long recipientUserId, BigDecimal amount, String currency, String senderAccount) {
        publishNotificationEvent(
                "transfer-completed",
                recipientUserId,
                "TRANSFER_RECEIVED",
                "Transfer Received",
                String.format("You received %s %s from account %s.", amount, currency, senderAccount)
        );
    }
    
    /**
     * Publishes transfer failed event.
     */
    public void publishTransferFailedEvent(Long senderUserId, BigDecimal amount, String currency, String reason) {
        publishNotificationEvent(
                "transfer-failed",
                senderUserId,
                "TRANSFER_FAILED",
                "Transfer Failed",
                String.format("Your transfer of %s %s failed: %s", amount, currency, reason)
        );
    }
}

