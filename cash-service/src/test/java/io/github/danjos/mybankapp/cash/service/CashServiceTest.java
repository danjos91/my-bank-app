package io.github.danjos.mybankapp.cash.service;

import io.github.danjos.mybankapp.cash.client.AccountsClient;
import io.github.danjos.mybankapp.cash.client.BlockerClient;
import io.github.danjos.mybankapp.cash.kafka.KafkaNotificationProducer;
import io.github.danjos.mybankapp.cash.dto.AccountDTO;
import io.github.danjos.mybankapp.cash.dto.CashTransactionDTO;
import io.github.danjos.mybankapp.cash.dto.DepositRequestDTO;
import io.github.danjos.mybankapp.cash.dto.WithdrawalRequestDTO;
import io.github.danjos.mybankapp.cash.dto.BlockResponseDTO;
import io.github.danjos.mybankapp.cash.entity.CashTransaction;
import io.github.danjos.mybankapp.cash.entity.Currency;
import io.github.danjos.mybankapp.cash.repository.CashTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private CashTransactionRepository cashTransactionRepository;

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private KafkaNotificationProducer kafkaNotificationProducer;

    @Mock
    private BlockerClient blockerClient;

    @InjectMocks
    private CashService cashService;

    private DepositRequestDTO depositRequest;
    private WithdrawalRequestDTO withdrawalRequest;
    private CashTransaction testTransaction;

    @BeforeEach
    void setUp() {
        depositRequest = DepositRequestDTO.builder()
                .accountId(1L)
                .amount(new BigDecimal("100.00"))
                .description("Test deposit")
                .build();

        withdrawalRequest = WithdrawalRequestDTO.builder()
                .accountId(1L)
                .amount(new BigDecimal("50.00"))
                .description("Test withdrawal")
                .build();

        testTransaction = CashTransaction.builder()
                .id(1L)
                .accountId(1L)
                .amount(new BigDecimal("100.00"))
                .transactionType(CashTransaction.TransactionType.DEPOSIT)
                .description("Test deposit")
                .timestamp(LocalDateTime.now())
                .build();

        BlockResponseDTO approvedResponse = BlockResponseDTO.builder()
                .decision(BlockResponseDTO.Decision.APPROVED)
                .build();
        lenient().when(blockerClient.checkTransaction(any(BigDecimal.class), anyString()))
                .thenReturn(approvedResponse);
    }

    @Test
    void deposit_ValidRequest_ReturnsTransactionDTO() {
        // Given
        AccountDTO accountDTO = AccountDTO.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .currency(Currency.RUB)
                .balance(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(accountsClient.getAccount(1L)).thenReturn(accountDTO);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(testTransaction);
        doNothing().when(accountsClient).addToAccountBalance(anyLong(), any(BigDecimal.class));
        doNothing().when(kafkaNotificationProducer).publishDepositCompletedEvent(anyLong(), anyString(), any(), anyString());

        // When
        CashTransactionDTO result = cashService.deposit(depositRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getAccountId());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(CashTransaction.TransactionType.DEPOSIT, result.getTransactionType());
        verify(cashTransactionRepository).save(any(CashTransaction.class));
        verify(accountsClient).addToAccountBalance(1L, new BigDecimal("100.00"));
        verify(kafkaNotificationProducer).publishDepositCompletedEvent(anyLong(), anyString(), any(), anyString());
    }

    @Test
    void deposit_AccountsClientThrowsException_ThrowsRuntimeException() {
        // Given
        AccountDTO accountDTO = AccountDTO.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .currency(Currency.RUB)
                .balance(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(accountsClient.getAccount(1L)).thenReturn(accountDTO);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(testTransaction);
        doThrow(new RuntimeException("Account service unavailable"))
                .when(accountsClient).addToAccountBalance(anyLong(), any(BigDecimal.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cashService.deposit(depositRequest));
        assertTrue(exception.getMessage().contains("Deposit failed"));
        verify(cashTransactionRepository).save(any(CashTransaction.class));
        verify(accountsClient).addToAccountBalance(1L, new BigDecimal("100.00"));
    }

    @Test
    void withdraw_ValidRequestWithSufficientBalance_ReturnsTransactionDTO() {
        // Given
        AccountDTO accountDTO = AccountDTO.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .currency(Currency.RUB)
                .balance(new BigDecimal("200.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(accountsClient.getAccount(1L)).thenReturn(accountDTO);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(testTransaction);
        doNothing().when(accountsClient).subtractFromAccountBalance(anyLong(), any(BigDecimal.class));
        doNothing().when(kafkaNotificationProducer).publishWithdrawalCompletedEvent(anyLong(), anyString(), any(), anyString());

        // When
        CashTransactionDTO result = cashService.withdraw(withdrawalRequest);

        // Then
        assertNotNull(result);
        verify(accountsClient).getAccount(1L);
        verify(cashTransactionRepository).save(any(CashTransaction.class));
        verify(accountsClient).subtractFromAccountBalance(1L, new BigDecimal("50.00"));
        verify(kafkaNotificationProducer).publishWithdrawalCompletedEvent(anyLong(), anyString(), any(), anyString());
    }

    @Test
    void withdraw_InsufficientBalance_ThrowsIllegalArgumentException() {
        // Given
        AccountDTO accountDTO = AccountDTO.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .currency(Currency.RUB)
                .balance(new BigDecimal("30.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(accountsClient.getAccount(1L)).thenReturn(accountDTO);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cashService.withdraw(withdrawalRequest));
        assertTrue(exception.getMessage().contains("Insufficient balance"));
        verify(accountsClient).getAccount(1L);
        verify(cashTransactionRepository, never()).save(any(CashTransaction.class));
        verify(accountsClient, never()).subtractFromAccountBalance(anyLong(), any(BigDecimal.class));
    }

    @Test
    void withdraw_AccountsClientThrowsException_ThrowsRuntimeException() {
        // Given
        AccountDTO accountDTO = AccountDTO.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .currency(Currency.RUB)
                .balance(new BigDecimal("200.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(accountsClient.getAccount(1L)).thenReturn(accountDTO);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(testTransaction);
        doThrow(new RuntimeException("Account service unavailable"))
                .when(accountsClient).subtractFromAccountBalance(anyLong(), any(BigDecimal.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cashService.withdraw(withdrawalRequest));
        assertTrue(exception.getMessage().contains("Withdrawal failed"));
        verify(accountsClient).getAccount(1L);
        verify(cashTransactionRepository).save(any(CashTransaction.class));
        verify(accountsClient).subtractFromAccountBalance(1L, new BigDecimal("50.00"));
    }

    @Test
    void getTransactionsByAccountId_ValidAccountId_ReturnsList() {
        // Given
        List<CashTransaction> transactions = Arrays.asList(testTransaction);
        when(cashTransactionRepository.findByAccountIdOrderByTimestampDesc(1L)).thenReturn(transactions);

        // When
        List<CashTransactionDTO> result = cashService.getTransactionsByAccountId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(cashTransactionRepository).findByAccountIdOrderByTimestampDesc(1L);
    }

    @Test
    void getTransactionsByAccountId_WithPageable_ReturnsPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<CashTransaction> transactionPage = new PageImpl<>(Arrays.asList(testTransaction));
        when(cashTransactionRepository.findByAccountIdOrderByTimestampDesc(1L, pageable)).thenReturn(transactionPage);

        // When
        Page<CashTransactionDTO> result = cashService.getTransactionsByAccountId(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(cashTransactionRepository).findByAccountIdOrderByTimestampDesc(1L, pageable);
    }

    @Test
    void getTransactionsByAccountIdAndType_ValidParameters_ReturnsList() {
        // Given
        List<CashTransaction> transactions = Arrays.asList(testTransaction);
        when(cashTransactionRepository.findByAccountIdAndTransactionTypeOrderByTimestampDesc(
                1L, CashTransaction.TransactionType.DEPOSIT)).thenReturn(transactions);

        // When
        List<CashTransactionDTO> result = cashService.getTransactionsByAccountIdAndType(
                1L, CashTransaction.TransactionType.DEPOSIT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(cashTransactionRepository).findByAccountIdAndTransactionTypeOrderByTimestampDesc(
                1L, CashTransaction.TransactionType.DEPOSIT);
    }

    @Test
    void getTransactionById_ValidId_ReturnsOptional() {
        // Given
        when(cashTransactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // When
        Optional<CashTransactionDTO> result = cashService.getTransactionById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(cashTransactionRepository).findById(1L);
    }

    @Test
    void getTransactionById_InvalidId_ReturnsEmpty() {
        // Given
        when(cashTransactionRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<CashTransactionDTO> result = cashService.getTransactionById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(cashTransactionRepository).findById(1L);
    }

    @Test
    void getTotalDepositsByAccountId_ValidAccountId_ReturnsTotal() {
        // Given
        BigDecimal totalDeposits = new BigDecimal("500.00");
        when(cashTransactionRepository.getTotalAmountByAccountIdAndTransactionType(
                1L, CashTransaction.TransactionType.DEPOSIT)).thenReturn(totalDeposits);

        // When
        BigDecimal result = cashService.getTotalDepositsByAccountId(1L);

        // Then
        assertEquals(totalDeposits, result);
        verify(cashTransactionRepository).getTotalAmountByAccountIdAndTransactionType(
                1L, CashTransaction.TransactionType.DEPOSIT);
    }

    @Test
    void getTotalWithdrawalsByAccountId_ValidAccountId_ReturnsTotal() {
        // Given
        BigDecimal totalWithdrawals = new BigDecimal("200.00");
        when(cashTransactionRepository.getTotalAmountByAccountIdAndTransactionType(
                1L, CashTransaction.TransactionType.WITHDRAWAL)).thenReturn(totalWithdrawals);

        // When
        BigDecimal result = cashService.getTotalWithdrawalsByAccountId(1L);

        // Then
        assertEquals(totalWithdrawals, result);
        verify(cashTransactionRepository).getTotalAmountByAccountIdAndTransactionType(
                1L, CashTransaction.TransactionType.WITHDRAWAL);
    }

    @Test
    void getAllTransactions_ReturnsAllTransactions() {
        // Given
        List<CashTransaction> transactions = Arrays.asList(testTransaction);
        when(cashTransactionRepository.findAll()).thenReturn(transactions);

        // When
        List<CashTransactionDTO> result = cashService.getAllTransactions();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(cashTransactionRepository).findAll();
    }
}

