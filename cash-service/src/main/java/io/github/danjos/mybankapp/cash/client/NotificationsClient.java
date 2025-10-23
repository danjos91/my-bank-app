package io.github.danjos.mybankapp.cash.client;

import io.github.danjos.mybankapp.cash.dto.CreateNotificationDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
    public void createNotification(CreateNotificationDTO notificationDTO) {
        try {
            String url = notificationsServiceUrl + "/api/notifications";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<CreateNotificationDTO> request = new HttpEntity<>(notificationDTO, headers);
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
        } catch (Exception e) {
            log.error("Error creating notification: {}", e.getMessage());
            throw e;
        }
    }
    
    @CircuitBreaker(name = "notifications-service", fallbackMethod = "createQuickNotificationFallback")
    @Retry(name = "notifications-service")
    public void createQuickNotification(Long userId, String type, String title, String message) {
        try {
            String url = notificationsServiceUrl + "/api/notifications/quick?userId=" + userId + 
                        "&type=" + type + "&title=" + title + "&message=" + message;
            restTemplate.postForEntity(url, null, Void.class);
        } catch (Exception e) {
            log.error("Error creating quick notification: {}", e.getMessage());
            throw e;
        }
    }
    
    // Fallback methods
    public void createNotificationFallback(CreateNotificationDTO notificationDTO, Exception ex) {
        log.warn("Fallback: Unable to create notification for user {}", notificationDTO.getUserId());
        // In a real scenario, you might want to queue this for later processing
    }
    
    public void createQuickNotificationFallback(Long userId, String type, String title, String message, Exception ex) {
        log.warn("Fallback: Unable to create quick notification for user {}", userId);
        // In a real scenario, you might want to queue this for later processing
    }
}
