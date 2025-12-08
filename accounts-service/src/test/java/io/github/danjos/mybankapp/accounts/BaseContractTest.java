package io.github.danjos.mybankapp.accounts;

import io.github.danjos.mybankapp.accounts.entity.Account;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.repository.AccountRepository;
import io.github.danjos.mybankapp.accounts.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.mockMvc;

@SpringBootTest(classes = {AccountsServiceApplication.class, io.github.danjos.mybankapp.accounts.config.TestSecurityConfig.class})
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
    protected UserRepository userRepository;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected User testUser;
    protected Account testAccount;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        // Setup RestAssuredMockMvc
        mockMvc(mockMvc);

        // Clean up before each test
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Reset sequences to ensure IDs start from 1
        // Use TRUNCATE to reset sequences automatically
        jdbcTemplate.execute("TRUNCATE TABLE accounts_schema.accounts RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE accounts_schema.users RESTART IDENTITY CASCADE");

        // Create a test user with ID 1
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);
        userRepository.flush();

        // Check if this is the createAccount test - if so, don't create an account
        // For other contract tests that need an account, create it here
        String testMethodName = testInfo.getTestMethod().map(m -> m.getName()).orElse("");
        if (!testMethodName.contains("createAccount")) {
            // Create a test account with ID 1 for contract tests that need it
            testAccount = Account.builder()
                    .user(testUser)
                    .balance(new BigDecimal("100.00"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            testAccount = accountRepository.save(testAccount);
            accountRepository.flush();
        }
    }
}
