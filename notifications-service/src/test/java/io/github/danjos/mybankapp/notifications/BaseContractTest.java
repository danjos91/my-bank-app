package io.github.danjos.mybankapp.notifications;

import io.github.danjos.mybankapp.notifications.entity.Notification;
import io.github.danjos.mybankapp.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

    protected Notification testNotification;

    @BeforeEach
    void setUp() {
        // Setup RestAssuredMockMvc
        mockMvc(mockMvc);

        // Clean up before each test
        notificationRepository.deleteAll();

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
