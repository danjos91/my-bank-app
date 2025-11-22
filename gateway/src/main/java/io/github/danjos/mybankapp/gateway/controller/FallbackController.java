package io.github.danjos.mybankapp.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        log.warn("Auth service fallback triggered");
        return createFallbackResponse("Auth service is temporarily unavailable", "AUTH_SERVICE_DOWN");
    }

    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> accountsFallback() {
        log.warn("Accounts service fallback triggered");
        return createFallbackResponse("Accounts service is temporarily unavailable", "ACCOUNTS_SERVICE_DOWN");
    }

    @GetMapping("/cash")
    public ResponseEntity<Map<String, Object>> cashFallback() {
        log.warn("Cash service fallback triggered");
        return createFallbackResponse("Cash service is temporarily unavailable", "CASH_SERVICE_DOWN");
    }

    @GetMapping("/transfers")
    public ResponseEntity<Map<String, Object>> transfersFallback() {
        log.warn("Transfer service fallback triggered");
        return createFallbackResponse("Transfer service is temporarily unavailable", "TRANSFER_SERVICE_DOWN");
    }

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notificationsFallback() {
        log.warn("Notifications service fallback triggered");
        return createFallbackResponse("Notifications service is temporarily unavailable", "NOTIFICATIONS_SERVICE_DOWN");
    }

    @GetMapping("/ui")
    public ResponseEntity<Map<String, Object>> uiFallback() {
        log.warn("Front UI fallback triggered");
        return createFallbackResponse("Front UI is temporarily unavailable", "UI_SERVICE_DOWN");
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthFallback() {
        log.warn("Health check fallback triggered");
        return createFallbackResponse("Health check service is temporarily unavailable", "HEALTH_SERVICE_DOWN");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorCode);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("retryAfter", "30 seconds");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
