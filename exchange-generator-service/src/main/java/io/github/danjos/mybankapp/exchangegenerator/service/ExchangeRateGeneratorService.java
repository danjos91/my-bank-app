package io.github.danjos.mybankapp.exchangegenerator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danjos.mybankapp.exchangegenerator.dto.ExchangeRateData;
import io.github.danjos.mybankapp.exchangegenerator.kafka.KafkaExchangeRateProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateGeneratorService {

    private final ObjectMapper objectMapper;
    private final KafkaExchangeRateProducer kafkaExchangeRateProducer;

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
        // Publish USD rate using Round Robin
        if (usdRates != null && !usdRates.isEmpty()) {
            int usdIdx = usdIndex.getAndIncrement() % usdRates.size();
            ExchangeRateData usdRate = usdRates.get(usdIdx);
            publishRate("USD", usdRate);
        }

        // Publish CNY rate using Round Robin
        if (cnyRates != null && !cnyRates.isEmpty()) {
            int cnyIdx = cnyIndex.getAndIncrement() % cnyRates.size();
            ExchangeRateData cnyRate = cnyRates.get(cnyIdx);
            publishRate("CNY", cnyRate);
        }
    }

    private void publishRate(String currency, ExchangeRateData rate) {
        try {
            kafkaExchangeRateProducer.publishRate(currency, rate.getBuyRate(), rate.getSellRate());
            log.debug("Queued {} rate event: buy={}, sell={}", currency, rate.getBuyRate(), rate.getSellRate());
        } catch (Exception e) {
            log.error("Failed to publish {} rate event: {}", currency, e.getMessage());
        }
    }
}

