package io.github.danjos.mybankapp.cash.client;

import io.github.danjos.mybankapp.cash.dto.BlockRequestDTO;
import io.github.danjos.mybankapp.cash.dto.BlockResponseDTO;
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
public class BlockerClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${services.blocker.url:http://localhost:8089}")
    private String blockerServiceUrl;
    
    @CircuitBreaker(name = "blocker-service", fallbackMethod = "checkTransactionFallback")
    @Retry(name = "blocker-service")
    public BlockResponseDTO checkTransaction(BigDecimal amount, String currency) {
        try {
            String url = blockerServiceUrl + "/api/blocker/check";
            
            BlockRequestDTO request = BlockRequestDTO.builder()
                    .serviceName("cash-service")
                    .amount(amount)
                    .currency(currency)
                    .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<BlockRequestDTO> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<BlockResponseDTO> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, BlockResponseDTO.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error checking transaction with blocker: {}", e.getMessage());
            throw e;
        }
    }
    
    // Fallback method - approve transaction if blocker is unavailable
    public BlockResponseDTO checkTransactionFallback(BigDecimal amount, String currency, Exception ex) {
        log.warn("Fallback: Blocker service unavailable, approving transaction");
        return BlockResponseDTO.builder()
                .decision(BlockResponseDTO.Decision.APPROVED)
                .reason("Blocker service unavailable")
                .build();
    }
}

