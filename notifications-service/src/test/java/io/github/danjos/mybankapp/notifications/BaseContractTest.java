package io.github.danjos.mybankapp.notifications;

import io.github.danjos.mybankapp.notifications.entity.Notification;
import io.github.danjos.mybankapp.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.mockMvc;

@SpringBootTest(classes = {NotificationsServiceApplication.class, io.github.danjos.mybankapp.notifications.config.TestSecurityConfig.class})
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public abstract class BaseContractTest {

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    protected Notification testNotification;

    @BeforeEach
    void setUp() {
        // Setup RestAssuredMockMvc
        mockMvc(mockMvc);

        // Clean up before each test and reset identity to satisfy contract expectations (id=1)
        try {
            jdbcTemplate.execute("TRUNCATE TABLE notifications_schema.notifications_log");
            jdbcTemplate.execute("ALTER TABLE notifications_schema.notifications_log ALTER COLUMN id RESTART WITH 1");
        } catch (DataAccessException ex) {
            // Fallback for H2 compatibility if truncate fails
            notificationRepository.deleteAll();
            jdbcTemplate.execute("ALTER TABLE notifications_schema.notifications_log ALTER COLUMN id RESTART WITH 1");
        }

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
