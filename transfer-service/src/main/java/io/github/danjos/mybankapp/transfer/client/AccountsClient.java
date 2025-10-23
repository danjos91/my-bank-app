package io.github.danjos.mybankapp.transfer.client;

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

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountsClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${services.accounts.url:http://localhost:8081}")
    private String accountsServiceUrl;
    
    @CircuitBreaker(name = "accounts-service", fallbackMethod = "getBalanceFallback")
    @Retry(name = "accounts-service")
    public BigDecimal getAccountBalance(Long accountId) {
        try {
            String url = accountsServiceUrl + "/api/accounts/" + accountId + "/balance";
            ResponseEntity<BigDecimal> response = restTemplate.getForEntity(url, BigDecimal.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting account balance for account {}: {}", accountId, e.getMessage());
            throw e;
        }
    }
    
    @CircuitBreaker(name = "accounts-service", fallbackMethod = "subtractFromBalanceFallback")
    @Retry(name = "accounts-service")
    public void subtractFromAccountBalance(Long accountId, BigDecimal amount) {
        try {
            String url = accountsServiceUrl + "/api/accounts/" + accountId + "/subtract";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<BigDecimal> request = new HttpEntity<>(amount, headers);
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
        } catch (Exception e) {
            log.error("Error subtracting from account balance for account {}: {}", accountId, e.getMessage());
            throw e;
        }
    }
    
    @CircuitBreaker(name = "accounts-service", fallbackMethod = "addToBalanceFallback")
    @Retry(name = "accounts-service")
    public void addToAccountBalance(Long accountId, BigDecimal amount) {
        try {
            String url = accountsServiceUrl + "/api/accounts/" + accountId + "/add";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<BigDecimal> request = new HttpEntity<>(amount, headers);
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
        } catch (Exception e) {
            log.error("Error adding to account balance for account {}: {}", accountId, e.getMessage());
            throw e;
        }
    }
    
    @CircuitBreaker(name = "accounts-service", fallbackMethod = "validateAccountFallback")
    @Retry(name = "accounts-service")
    public boolean validateAccountExists(Long accountId) {
        try {
            String url = accountsServiceUrl + "/api/accounts/" + accountId;
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error validating account {}: {}", accountId, e.getMessage());
            return false;
        }
    }
    
    // Fallback methods
    public BigDecimal getBalanceFallback(Long accountId, Exception ex) {
        log.warn("Fallback: Unable to get balance for account {}, returning 0", accountId);
        return BigDecimal.ZERO;
    }
    
    public void subtractFromBalanceFallback(Long accountId, BigDecimal amount, Exception ex) {
        log.warn("Fallback: Unable to subtract from balance for account {}", accountId);
        throw new RuntimeException("Unable to subtract from account balance");
    }
    
    public void addToBalanceFallback(Long accountId, BigDecimal amount, Exception ex) {
        log.warn("Fallback: Unable to add to balance for account {}", accountId);
        throw new RuntimeException("Unable to add to account balance");
    }
    
    public boolean validateAccountFallback(Long accountId, Exception ex) {
        log.warn("Fallback: Unable to validate account {}", accountId);
        return false;
    }
}
