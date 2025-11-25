package io.github.danjos.mybankapp.transfer.client;

import io.github.danjos.mybankapp.transfer.dto.ConversionRequestDTO;
import io.github.danjos.mybankapp.transfer.dto.ConversionResponseDTO;
import io.github.danjos.mybankapp.transfer.entity.Currency;
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
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${services.exchange.url:http://localhost:8087}")
    private String exchangeServiceUrl;
    
    @CircuitBreaker(name = "exchange-service", fallbackMethod = "convertFallback")
    @Retry(name = "exchange-service")
    public ConversionResponseDTO convert(ConversionRequestDTO request) {
        try {
            String url = exchangeServiceUrl + "/api/exchange/convert";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            // Convert to exchange-service DTO format
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("fromCurrency", request.getFromCurrency().name());
            requestBody.put("toCurrency", request.getToCurrency().name());
            requestBody.put("amount", request.getAmount());
            
            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = 
                    restTemplate.exchange(url, HttpMethod.POST, httpRequest, Map.class);
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response from exchange service");
            }
            
            // Convert response back to TransferService DTO
            return ConversionResponseDTO.builder()
                    .fromCurrency(Currency.valueOf((String) responseBody.get("fromCurrency")))
                    .toCurrency(Currency.valueOf((String) responseBody.get("toCurrency")))
                    .originalAmount(new BigDecimal(responseBody.get("originalAmount").toString()))
                    .convertedAmount(new BigDecimal(responseBody.get("convertedAmount").toString()))
                    .exchangeRate(new BigDecimal(responseBody.get("exchangeRate").toString()))
                    .build();
        } catch (Exception e) {
            log.error("Error converting currency: {}", e.getMessage());
            throw e;
        }
    }
    
    public ConversionResponseDTO convertFallback(ConversionRequestDTO request, Exception ex) {
        log.warn("Fallback: Unable to convert currency, returning original amount");
        // Fallback: return original amount without conversion
        return ConversionResponseDTO.builder()
                .fromCurrency(request.getFromCurrency())
                .toCurrency(request.getToCurrency())
                .originalAmount(request.getAmount())
                .convertedAmount(request.getAmount())
                .exchangeRate(BigDecimal.ONE)
                .build();
    }
}

