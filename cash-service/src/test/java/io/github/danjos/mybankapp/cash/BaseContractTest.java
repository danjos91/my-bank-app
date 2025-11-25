package io.github.danjos.mybankapp.cash;

import io.github.danjos.mybankapp.cash.client.AccountsClient;
import io.github.danjos.mybankapp.cash.client.NotificationsClient;
import io.github.danjos.mybankapp.cash.dto.AccountDTO;
import io.github.danjos.mybankapp.cash.entity.CashTransaction;
import io.github.danjos.mybankapp.cash.entity.Currency;
import io.github.danjos.mybankapp.cash.repository.CashTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

@SpringBootTest(classes = {CashServiceApplication.class, io.github.danjos.mybankapp.cash.config.TestJpaConfig.class, io.github.danjos.mybankapp.cash.config.TestSecurityConfig.class})
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
    protected CashTransactionRepository cashTransactionRepository;

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected AccountsClient accountsClient;

    @MockBean
    protected NotificationsClient notificationsClient;

    protected CashTransaction testTransaction;

    @BeforeEach
    void setUp() {
        // Setup RestAssuredMockMvc
        mockMvc(mockMvc);

        // Mock AccountDTO for getAccount calls
        AccountDTO accountDTO = AccountDTO.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .currency(Currency.RUB)
                .balance(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Mock external clients
        when(accountsClient.getAccount(anyLong())).thenReturn(accountDTO);
        when(accountsClient.getAccountBalance(anyLong())).thenReturn(new BigDecimal("1000.00"));
        doNothing().when(accountsClient).addToAccountBalance(anyLong(), any(BigDecimal.class));
        doNothing().when(accountsClient).subtractFromAccountBalance(anyLong(), any(BigDecimal.class));
        doNothing().when(notificationsClient).createNotification(any());

        // Clean up before each test
        cashTransactionRepository.deleteAll();

        // Create a test transaction for getTransactionsByAccountId test
        testTransaction = CashTransaction.builder()
                .accountId(1L)
                .amount(new BigDecimal("100.00"))
                .transactionType(CashTransaction.TransactionType.DEPOSIT)
                .description("Test deposit")
                .timestamp(LocalDateTime.now())
                .build();
        testTransaction = cashTransactionRepository.save(testTransaction);
    }
}
