package io.github.danjos.mybankapp.transfer;

import io.github.danjos.mybankapp.transfer.client.AccountsClient;
import io.github.danjos.mybankapp.transfer.client.BlockerClient;
import io.github.danjos.mybankapp.transfer.client.ExchangeClient;
import io.github.danjos.mybankapp.transfer.kafka.KafkaNotificationProducer;
import io.github.danjos.mybankapp.transfer.dto.AccountDTO;
import io.github.danjos.mybankapp.transfer.dto.BlockResponseDTO;
import io.github.danjos.mybankapp.transfer.dto.ConversionRequestDTO;
import io.github.danjos.mybankapp.transfer.dto.ConversionResponseDTO;
import io.github.danjos.mybankapp.transfer.entity.Currency;
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
import static org.mockito.ArgumentMatchers.anyString;
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
    protected KafkaNotificationProducer kafkaNotificationProducer;

    @MockBean
    protected ExchangeClient exchangeClient;

    @MockBean
    protected BlockerClient blockerClient;

    protected Transfer testTransfer;
    
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Setup RestAssuredMockMvc
        mockMvc(mockMvc);

        // Mock AccountDTO for getAccount calls
        AccountDTO account1 = AccountDTO.builder()
                .id(1L)
                .userId(1L)
                .username("user1")
                .currency(Currency.RUB)
                .balance(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AccountDTO account2 = AccountDTO.builder()
                .id(2L)
                .userId(2L)
                .username("user2")
                .currency(Currency.RUB)
                .balance(new BigDecimal("500.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Mock external clients
        when(accountsClient.getAccountBalance(anyLong())).thenReturn(new BigDecimal("1000.00"));
        when(accountsClient.validateAccountExists(anyLong())).thenReturn(true);
        when(accountsClient.getAccount(1L)).thenReturn(account1);
        when(accountsClient.getAccount(2L)).thenReturn(account2);
        // Default mock for any other account ID
        when(accountsClient.getAccount(anyLong())).thenAnswer(invocation -> {
            Long accountId = invocation.getArgument(0);
            return AccountDTO.builder()
                    .id(accountId)
                    .userId(accountId)
                    .username("user" + accountId)
                    .currency(Currency.RUB)
                    .balance(new BigDecimal("1000.00"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        });
        doNothing().when(accountsClient).subtractFromAccountBalance(anyLong(), any(BigDecimal.class));
        doNothing().when(accountsClient).addToAccountBalance(anyLong(), any(BigDecimal.class));
        doNothing().when(kafkaNotificationProducer).publishTransferInitiatedEvent(anyLong(), any(), anyString());
        doNothing().when(kafkaNotificationProducer).publishTransferCompletedEvent(anyLong(), any(), anyString(), anyString());
        doNothing().when(kafkaNotificationProducer).publishTransferReceivedEvent(anyLong(), any(), anyString(), anyString());
        doNothing().when(kafkaNotificationProducer).publishTransferFailedEvent(anyLong(), any(), anyString(), anyString());

        // Mock exchange client - return same amount if currencies are the same
        when(exchangeClient.convert(any(ConversionRequestDTO.class))).thenAnswer(invocation -> {
            ConversionRequestDTO request = invocation.getArgument(0);
            return ConversionResponseDTO.builder()
                    .fromCurrency(request.getFromCurrency())
                    .toCurrency(request.getToCurrency())
                    .originalAmount(request.getAmount())
                    .convertedAmount(request.getAmount())
                    .exchangeRate(BigDecimal.ONE)
                    .build();
        });

        // Mock blocker client - approve all transactions
        when(blockerClient.checkTransaction(any(BigDecimal.class), anyString())).thenReturn(
                BlockResponseDTO.builder()
                        .decision(BlockResponseDTO.Decision.APPROVED)
                        .reason("Transaction approved")
                        .build()
        );

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
