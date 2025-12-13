package io.github.danjos.mybankapp.exchangegenerator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes exchange rate events to Kafka for consumption by exchange-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaExchangeRateProducer {

    private final KafkaTemplate<String, ExchangeRateEvent> kafkaTemplate;

    @Value("${kafka.topics.exchange-rates:exchange-rates}")
    private String exchangeRatesTopic;

    private static final String SOURCE_SERVICE = "exchange-generator-service";

    public void publishRate(String currency, java.math.BigDecimal buyRate, java.math.BigDecimal sellRate) {
        ExchangeRateEvent event = ExchangeRateEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .currency(currency)
                .buyRate(buyRate)
                .sellRate(sellRate)
                .sourceService(SOURCE_SERVICE)
                .timestamp(System.currentTimeMillis())
                .build();

        String key = currency != null ? currency : UUID.randomUUID().toString();

        CompletableFuture<SendResult<String, ExchangeRateEvent>> future =
                kafkaTemplate.send(exchangeRatesTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Published exchange rate event: topic={}, currency={}, partition={}, offset={}",
                        exchangeRatesTopic,
                        currency,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish exchange rate event for {}: {}", currency, ex.getMessage(), ex);
            }
        });
    }
}

