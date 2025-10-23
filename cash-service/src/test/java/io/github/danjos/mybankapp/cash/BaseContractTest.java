package io.github.danjos.mybankapp.cash;

import io.github.danjos.mybankapp.cash.entity.CashTransaction;
import io.github.danjos.mybankapp.cash.repository.CashTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest(classes = CashServiceApplication.class)
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
    protected CashTransactionRepository cashTransactionRepository;

    protected CashTransaction testTransaction;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        cashTransactionRepository.deleteAll();

        // Create a test transaction
        testTransaction = CashTransaction.builder()
                .accountId(1L)
                .amount(new BigDecimal("100.00"))
                .transactionType(CashTransaction.TransactionType.DEPOSIT)
                .description("Test deposit")
                .transactionDate(LocalDateTime.now())
                .build();
        testTransaction = cashTransactionRepository.save(testTransaction);
    }
}
