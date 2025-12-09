package io.github.danjos.mybankapp.cash.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for publishing notification events from cash-service.
 * Replaces REST-based NotificationsClient for asynchronous notification delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationProducer {
    
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private static final String SOURCE_SERVICE = "cash-service";
    
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
     * Publishes deposit completed event.
     */
    public void publishDepositCompletedEvent(Long userId, String accountNumber, BigDecimal amount, String currency) {
        publishNotificationEvent(
                "deposit-completed",
                userId,
                "DEPOSIT_SUCCESS",
                "Deposit Successful",
                String.format("Your deposit of %s %s to account %s was successful.", amount, currency, accountNumber)
        );
    }
    
    /**
     * Publishes withdrawal completed event.
     */
    public void publishWithdrawalCompletedEvent(Long userId, String accountNumber, BigDecimal amount, String currency) {
        publishNotificationEvent(
                "withdrawal-completed",
                userId,
                "WITHDRAWAL_SUCCESS",
                "Withdrawal Successful",
                String.format("Your withdrawal of %s %s from account %s was successful.", amount, currency, accountNumber)
        );
    }
}

