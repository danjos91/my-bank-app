package io.github.danjos.mybankapp.notifications;

import io.github.danjos.mybankapp.notifications.entity.Notification;
import io.github.danjos.mybankapp.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.mockMvc;

@SpringBootTest(classes = {NotificationsServiceApplication.class, io.github.danjos.mybankapp.notifications.config.TestSecurityConfig.class})
@Testcontainers
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public abstract class BaseContractTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected Notification testNotification;

    @BeforeEach
    void setUp() {
        // Setup RestAssuredMockMvc
        mockMvc(mockMvc);

        // Clean up before each test
        notificationRepository.deleteAll();
        jdbcTemplate.execute("TRUNCATE TABLE notifications_schema.notifications_log RESTART IDENTITY CASCADE");

        // Create a test notification
        testNotification = Notification.builder()
                .userId(1L)
                .notificationType(Notification.NotificationType.ACCOUNT_CREATED)
                .title("Test Notification")
                .message("This is a test notification")
                .isRead(false)
                .build();
        testNotification = notificationRepository.save(testNotification);
    }
}
