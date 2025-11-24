package io.github.danjos.mybankapp.exchangegenerator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danjos.mybankapp.exchangegenerator.dto.ExchangeRateData;
import io.github.danjos.mybankapp.exchangegenerator.dto.UpdateRateRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateGeneratorService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${services.exchange.url:http://localhost:8087}")
    private String exchangeServiceUrl;
    
    @Value("classpath:exchange-rates.json")
    private Resource exchangeRatesResource;
    
    private List<ExchangeRateData> usdRates;
    private List<ExchangeRateData> cnyRates;
    private final AtomicInteger usdIndex = new AtomicInteger(0);
    private final AtomicInteger cnyIndex = new AtomicInteger(0);
    
    @PostConstruct
    public void init() {
        try {
            loadExchangeRates();
            log.info("Loaded {} USD rates and {} CNY rates", usdRates.size(), cnyRates.size());
        } catch (IOException e) {
            log.error("Failed to load exchange rates from file", e);
            throw new RuntimeException("Failed to initialize exchange rate generator", e);
        }
    }
    
    private void loadExchangeRates() throws IOException {
        try (InputStream inputStream = exchangeRatesResource.getInputStream()) {
            List<ExchangeRateData> allRates = objectMapper.readValue(inputStream, new TypeReference<List<ExchangeRateData>>() {});
            
            // Separate rates by currency
            usdRates = allRates.stream()
                    .filter(rate -> "USD".equals(rate.getCurrency()))
                    .toList();
            
            cnyRates = allRates.stream()
                    .filter(rate -> "CNY".equals(rate.getCurrency()))
                    .toList();
        }
    }
    
    @Scheduled(fixedRate = 1000) // Every second
    public void generateAndUpdateRates() {
        // Update USD rate using Round Robin
        if (usdRates != null && !usdRates.isEmpty()) {
            int usdIdx = usdIndex.getAndIncrement() % usdRates.size();
            ExchangeRateData usdRate = usdRates.get(usdIdx);
            updateRate("USD", usdRate.getBuyRate(), usdRate.getSellRate());
        }
        
        // Update CNY rate using Round Robin
        if (cnyRates != null && !cnyRates.isEmpty()) {
            int cnyIdx = cnyIndex.getAndIncrement() % cnyRates.size();
            ExchangeRateData cnyRate = cnyRates.get(cnyIdx);
            updateRate("CNY", cnyRate.getBuyRate(), cnyRate.getSellRate());
        }
    }
    
    private void updateRate(String currency, BigDecimal buyRate, BigDecimal sellRate) {
        try {
            String url = exchangeServiceUrl + "/api/exchange/rates";
            
            UpdateRateRequestDTO request = UpdateRateRequestDTO.builder()
                    .currency(currency)
                    .buyRate(buyRate)
                    .sellRate(sellRate)
                    .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UpdateRateRequestDTO> entity = new HttpEntity<>(request, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            
            log.debug("Updated {} rate: buy={}, sell={}", currency, buyRate, sellRate);
        } catch (Exception e) {
            log.error("Failed to update {} rate: {}", currency, e.getMessage());
        }
    }
}

