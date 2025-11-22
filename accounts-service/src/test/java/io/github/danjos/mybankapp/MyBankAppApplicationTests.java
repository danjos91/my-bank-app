package io.github.danjos.mybankapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = {
        io.github.danjos.mybankapp.accounts.AccountsServiceApplication.class,
        io.github.danjos.mybankapp.accounts.config.TestSecurityConfig.class
    },
    properties = {
        "spring.test.context.cache.maxSize=1"
    }
)
@Testcontainers
@ActiveProfiles("test")
class MyBankAppApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Test
    void contextLoads() {
        // This test verifies that the Spring context loads successfully
    }

}
