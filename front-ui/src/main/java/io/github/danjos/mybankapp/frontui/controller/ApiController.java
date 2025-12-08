package io.github.danjos.mybankapp.frontui.controller;

import io.github.danjos.mybankapp.frontui.service.BankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final BankService bankService;

    @GetMapping("/accounts/username/{username}")
    public ResponseEntity<?> getUserAccounts(@PathVariable String username) {
        try {
            // Verify user is authenticated
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                log.warn("Unauthenticated request to get accounts for user: {}", username);
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            log.debug("Getting accounts for user: {} requested by: {}", username, auth.getName());
            
            // Get user accounts using BankService which will add JWT token
            List<Map<String, Object>> accounts = bankService.getUserAccounts(username);
            
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            log.error("Error getting accounts for user: {}", username, e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}

