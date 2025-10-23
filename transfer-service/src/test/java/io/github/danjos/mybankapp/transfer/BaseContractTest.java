package io.github.danjos.mybankapp.transfer;

import io.github.danjos.mybankapp.transfer.entity.Transfer;
import io.github.danjos.mybankapp.transfer.repository.TransferRepository;
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

@SpringBootTest(classes = TransferServiceApplication.class)
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
    protected TransferRepository transferRepository;

    protected Transfer testTransfer;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        transferRepository.deleteAll();

        // Create a test transfer
        testTransfer = Transfer.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("50.00"))
                .description("Test transfer")
                .status(Transfer.TransferStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testTransfer = transferRepository.save(testTransfer);
    }
}
