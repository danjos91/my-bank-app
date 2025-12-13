package io.github.danjos.mybankapp.notifications.kafka;

import io.github.danjos.mybankapp.notifications.NotificationsServiceApplication;
import io.github.danjos.mybankapp.notifications.repository.NotificationRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = NotificationsServiceApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"notification-event"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=notifications-service-group",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.datasource.url=jdbc:h2:mem:notifications;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=notifications_schema",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.sql.init.mode=never"
})
class KafkaNotificationConsumerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldConsumeNotificationEventAndPersist() throws Exception {
        notificationRepository.deleteAll();

        KafkaTemplate<String, NotificationEvent> template = kafkaTemplate();

        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(123L)
                .notificationType(io.github.danjos.mybankapp.notifications.entity.Notification.NotificationType.ACCOUNT_CREATED)
                .title("Account Created")
                .message("Your account is ready.")
                .sourceService("test-service")
                .timestamp(System.currentTimeMillis())
                .build();

        template.send("notification-event", event.getUserId().toString(), event).get(10, TimeUnit.SECONDS);

        // Simple await: poll repository for the saved notification
        long start = System.currentTimeMillis();
        while (notificationRepository.count() == 0 && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(100);
        }

        assertThat(notificationRepository.count()).isEqualTo(1);
        assertThat(notificationRepository.findAll().get(0).getUserId()).isEqualTo(123L);
    }

    private KafkaTemplate<String, NotificationEvent> kafkaTemplate() {
        Map<String, Object> props = KafkaTestUtils.producerProps(embeddedKafka);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonSerializer.class);
        ProducerFactory<String, NotificationEvent> pf = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(pf);
    }
}

