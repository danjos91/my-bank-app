package io.github.danjos.mybankapp.exchange.kafka;

import io.github.danjos.mybankapp.exchange.entity.Currency;
import io.github.danjos.mybankapp.exchange.config.KafkaConsumerConfig;
import io.github.danjos.mybankapp.exchange.service.ExchangeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {ExchangeRateKafkaListener.class, KafkaConsumerConfig.class})
@EmbeddedKafka(partitions = 1, topics = {"exchange-rates"})
@ActiveProfiles("test")
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
class ExchangeRateKafkaListenerTest {

    @Autowired
    private KafkaTemplate<String, ExchangeRateEvent> kafkaTemplate;

    @MockBean
    private ExchangeService exchangeService;

    @MockBean
    private io.github.danjos.mybankapp.exchange.metrics.ExchangeMetrics exchangeMetrics;

    @Test
    void shouldConsumeExchangeRateEventAndUpdateRate() throws Exception {
        ExchangeRateEvent event = ExchangeRateEvent.builder()
                .eventId("evt-1")
                .currency("USD")
                .buyRate(new BigDecimal("1.100000"))
                .sellRate(new BigDecimal("1.200000"))
                .sourceService("test")
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaTemplate.send("exchange-rates", "USD", event).get(10, TimeUnit.SECONDS);

        ArgumentMatcher<io.github.danjos.mybankapp.exchange.dto.UpdateRateRequestDTO> matcher =
                req -> req.getCurrency() == Currency.USD
                        && req.getBuyRate().compareTo(new BigDecimal("1.100000")) == 0
                        && req.getSellRate().compareTo(new BigDecimal("1.200000")) == 0;

        verify(exchangeService, timeout(10000).times(1))
                .updateRate(argThat(matcher));
    }
}

