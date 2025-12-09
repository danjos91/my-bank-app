package io.github.danjos.mybankapp.accounts.kafka;

import io.github.danjos.mybankapp.accounts.AccountsServiceApplication;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AccountsServiceApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"account-created"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class KafkaNotificationProducerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaNotificationProducer producer;

    private Consumer<String, NotificationEvent> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonDeserializer.class);
        props.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(org.springframework.kafka.support.serializer.JsonDeserializer.VALUE_DEFAULT_TYPE,
                NotificationEvent.class.getName());
        consumer = new DefaultKafkaConsumerFactory<String, NotificationEvent>(props).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "account-created");
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void publishAccountCreatedEvent_sendsMessageToKafka() {
        producer.publishAccountCreatedEvent(123L, "ACC-1", "RUB");

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);
        var record = records.iterator().next();
        assertThat(record.topic()).isEqualTo("account-created");
        assertThat(record.key()).isEqualTo("123");
        NotificationEvent event = (NotificationEvent) record.value();
        assertThat(event.getUserId()).isEqualTo(123L);
        assertThat(event.getNotificationType()).isEqualTo("ACCOUNT_CREATED");
    }
}

