package io.github.danjos.mybankapp.exchange.kafka;

import io.github.danjos.mybankapp.exchange.dto.UpdateRateRequestDTO;
import io.github.danjos.mybankapp.exchange.entity.Currency;
import io.github.danjos.mybankapp.exchange.metrics.ExchangeMetrics;
import io.github.danjos.mybankapp.exchange.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes exchange rate events and persists them via ExchangeService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateKafkaListener {

    private final ExchangeService exchangeService;
    private final ExchangeMetrics exchangeMetrics;

    @Value("${kafka.topics.exchange-rates:exchange-rates}")
    private String exchangeRatesTopic;

    @KafkaListener(
            topics = "${kafka.topics.exchange-rates:exchange-rates}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "exchangeRateKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload ExchangeRateEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack
    ) {
        try {
            log.debug("Received exchange rate event: topic={}, partition={}, offset={}, currency={}, buy={}, sell={}",
                    topic, partition, offset, event.getCurrency(), event.getBuyRate(), event.getSellRate());

            if (event.getCurrency() == null || event.getBuyRate() == null || event.getSellRate() == null) {
                log.error("Invalid exchange rate event, missing fields. eventId={}", event.getEventId());
                exchangeMetrics.recordInvalidRateEvent();
                ack.acknowledge();
                return;
            }

            Currency currency;
            try {
                currency = Currency.valueOf(event.getCurrency());
            } catch (IllegalArgumentException e) {
                log.error("Unknown currency '{}' in eventId={}", event.getCurrency(), event.getEventId());
                exchangeMetrics.recordInvalidRateEvent();
                ack.acknowledge();
                return;
            }

            UpdateRateRequestDTO request = UpdateRateRequestDTO.builder()
                    .currency(currency)
                    .buyRate(event.getBuyRate())
                    .sellRate(event.getSellRate())
                    .build();

            exchangeService.updateRate(request);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process exchange rate event. topic={}, eventId={}, error={}",
                    topic, event.getEventId(), e.getMessage(), e);
            exchangeMetrics.recordMissingRateEvent();
            throw e;
        }
    }
}

