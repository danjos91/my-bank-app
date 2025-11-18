package io.github.danjos.mybankapp.transfer;

import io.github.danjos.mybankapp.transfer.client.AccountsClient;
import io.github.danjos.mybankapp.transfer.client.NotificationsClient;
import io.github.danjos.mybankapp.transfer.entity.Transfer;
import io.github.danjos.mybankapp.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.mockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {TransferServiceApplication.class, io.github.danjos.mybankapp.transfer.config.TestSecurityConfig.class})
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
    protected TransferRepository transferRepository;

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected AccountsClient accountsClient;

    @MockBean
    protected NotificationsClient notificationsClient;

    protected Transfer testTransfer;
    
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Setup RestAssuredMockMvc
        mockMvc(mockMvc);

        // Mock external clients
        when(accountsClient.getAccountBalance(anyLong())).thenReturn(new BigDecimal("1000.00"));
        when(accountsClient.validateAccountExists(anyLong())).thenReturn(true);
        doNothing().when(accountsClient).subtractFromAccountBalance(anyLong(), any(BigDecimal.class));
        doNothing().when(accountsClient).addToAccountBalance(anyLong(), any(BigDecimal.class));
        doNothing().when(notificationsClient).createNotification(any());

        // Clean up before each test
        transferRepository.deleteAll();
        jdbcTemplate.execute("TRUNCATE TABLE transfer_schema.transfers RESTART IDENTITY CASCADE");

        // Create a test transfer
        testTransfer = Transfer.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("50.00"))
                .description("Test transfer")
                .status(Transfer.TransferStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        testTransfer = transferRepository.save(testTransfer);
    }
}
