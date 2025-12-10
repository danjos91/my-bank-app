package io.github.danjos.mybankapp.exchangegenerator.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = KafkaExchangeRateProducer.class)
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@EmbeddedKafka(partitions = 1, topics = {"exchange-rates"})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.acks=0",
        "spring.kafka.producer.retries=0",
        "spring.kafka.producer.properties.enable.idempotence=false",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false",
        "kafka.topics.exchange-rates=exchange-rates"
})
class KafkaExchangeRateProducerTest {

    @org.springframework.beans.factory.annotation.Autowired
    private EmbeddedKafkaBroker embeddedKafka;
    private Consumer<String, ExchangeRateEvent> consumer;
    @org.springframework.beans.factory.annotation.Autowired
    private KafkaExchangeRateProducer producer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close(Duration.ofSeconds(1));
        }
    }

    @Test
    void shouldPublishExchangeRateEventOnce() {
        consumer = createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "exchange-rates");

        producer.publishRate("USD", new BigDecimal("1.10"), new BigDecimal("1.20"));

        ConsumerRecord<String, ExchangeRateEvent> record =
                KafkaTestUtils.getSingleRecord(consumer, "exchange-rates", Duration.ofSeconds(5));

        assertThat(record.key()).isEqualTo("USD");
        assertThat(record.value().getCurrency()).isEqualTo("USD");
        assertThat(record.value().getBuyRate()).isEqualByComparingTo(new BigDecimal("1.10"));
        assertThat(record.value().getSellRate()).isEqualByComparingTo(new BigDecimal("1.20"));

        assertThat(consumer.poll(Duration.ofMillis(200)).isEmpty()).isTrue();
    }

    private Consumer<String, ExchangeRateEvent> createConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("producer-test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonDeserializer.class);
        consumerProps.put(org.springframework.kafka.support.serializer.JsonDeserializer.VALUE_DEFAULT_TYPE,
                ExchangeRateEvent.class.getName());
        consumerProps.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(org.springframework.kafka.support.serializer.JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new org.springframework.kafka.support.serializer.JsonDeserializer<>(ExchangeRateEvent.class, false)
        ).createConsumer();
    }
}
