package io.github.danjos.mybankapp.notifications;

import io.github.danjos.mybankapp.notifications.entity.Notification;
import io.github.danjos.mybankapp.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

@SpringBootTest(classes = NotificationsServiceApplication.class)
@Testcontainers
@ActiveProfiles("test")
@Transactional
public abstract class BaseContractTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    protected NotificationRepository notificationRepository;

    protected Notification testNotification;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        notificationRepository.deleteAll();

        // Create a test notification
        testNotification = Notification.builder()
                .userId(1L)
                .type(Notification.NotificationType.INFO)
                .title("Test Notification")
                .message("This is a test notification")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testNotification = notificationRepository.save(testNotification);
    }
}
