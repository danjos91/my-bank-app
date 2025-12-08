package io.github.danjos.mybankapp.accounts.client;

import io.github.danjos.mybankapp.accounts.dto.CreateNotificationDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationsClient {

    private final RestTemplate restTemplate;

    @Value("${services.notifications.url:http://localhost:8084}")
    private String notificationsServiceUrl;

    @CircuitBreaker(name = "notifications-service", fallbackMethod = "createNotificationFallback")
    @Retry(name = "notifications-service")
    public void createNotification(CreateNotificationDTO notification) {
        try {
            String url = notificationsServiceUrl + "/api/notifications";
            restTemplate.postForObject(url, notification, Void.class);
        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage());
            // Don't rethrow, just log
        }
    }

    public void createNotificationFallback(CreateNotificationDTO notification, Exception ex) {
        log.warn("Fallback: Unable to send notification for user {}", notification.getUserId());
    }
}

