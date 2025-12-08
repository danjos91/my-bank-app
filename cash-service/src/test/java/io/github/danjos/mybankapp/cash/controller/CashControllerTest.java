package io.github.danjos.mybankapp.cash.controller;

import io.github.danjos.mybankapp.cash.dto.CashTransactionDTO;
import io.github.danjos.mybankapp.cash.dto.DepositRequestDTO;
import io.github.danjos.mybankapp.cash.dto.WithdrawalRequestDTO;
import io.github.danjos.mybankapp.cash.entity.CashTransaction;
import io.github.danjos.mybankapp.cash.exception.GlobalExceptionHandler;
import io.github.danjos.mybankapp.cash.service.CashService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashControllerTest {

    @Mock
    private CashService cashService;

    @InjectMocks
    private CashController cashController;
    
    private GlobalExceptionHandler exceptionHandler;

    private DepositRequestDTO depositRequest;
    private WithdrawalRequestDTO withdrawalRequest;
    private CashTransactionDTO transactionDTO;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        
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

        transactionDTO = CashTransactionDTO.builder()
                .id(1L)
                .accountId(1L)
                .amount(new BigDecimal("100.00"))
                .transactionType(CashTransaction.TransactionType.DEPOSIT)
                .description("Test deposit")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    private ResponseEntity<?> invokeControllerWithExceptionHandling(java.util.function.Supplier<ResponseEntity<?>> controllerMethod) {
        try {
            return controllerMethod.get();
        } catch (IllegalArgumentException e) {
            return exceptionHandler.handleIllegalArgumentException(e);
        } catch (Exception e) {
            return exceptionHandler.handleException(e);
        }
    }

    @Test
    void deposit_ValidRequest_ReturnsCreated() {
        // Given
        when(cashService.deposit(any(DepositRequestDTO.class))).thenReturn(transactionDTO);

        // When
        ResponseEntity<?> response = cashController.deposit(depositRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CashTransactionDTO);
        verify(cashService).deposit(depositRequest);
    }

    @Test
    void deposit_IllegalArgumentException_ReturnsBadRequest() {
        // Given
        when(cashService.deposit(any(DepositRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid request"));

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> cashController.deposit(depositRequest));

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request", response.getBody());
        verify(cashService).deposit(depositRequest);
    }

    @Test
    void deposit_GenericException_ReturnsInternalServerError() {
        // Given
        when(cashService.deposit(any(DepositRequestDTO.class)))
                .thenThrow(new RuntimeException("Service error"));

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> cashController.deposit(depositRequest));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Internal server error"));
        verify(cashService).deposit(depositRequest);
    }

    @Test
    void withdraw_ValidRequest_ReturnsCreated() {
        // Given
        when(cashService.withdraw(any(WithdrawalRequestDTO.class))).thenReturn(transactionDTO);

        // When
        ResponseEntity<?> response = cashController.withdraw(withdrawalRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CashTransactionDTO);
        verify(cashService).withdraw(withdrawalRequest);
    }

    @Test
    void withdraw_IllegalArgumentException_ReturnsBadRequest() {
        // Given
        when(cashService.withdraw(any(WithdrawalRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Insufficient balance"));

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> cashController.withdraw(withdrawalRequest));

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Insufficient balance", response.getBody());
        verify(cashService).withdraw(withdrawalRequest);
    }

    @Test
    void withdraw_GenericException_ReturnsInternalServerError() {
        // Given
        when(cashService.withdraw(any(WithdrawalRequestDTO.class)))
                .thenThrow(new RuntimeException("Service error"));

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> cashController.withdraw(withdrawalRequest));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Internal server error"));
        verify(cashService).withdraw(withdrawalRequest);
    }

    @Test
    void getTransactionsByAccountId_ValidAccountId_ReturnsOk() {
        // Given
        List<CashTransactionDTO> transactions = Arrays.asList(transactionDTO);
        when(cashService.getTransactionsByAccountId(1L)).thenReturn(transactions);

        // When
        ResponseEntity<?> response = cashController.getTransactionsByAccountId(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        verify(cashService).getTransactionsByAccountId(1L);
    }

    @Test
    void getTransactionsByAccountId_Exception_ReturnsInternalServerError() {
        // Given
        when(cashService.getTransactionsByAccountId(1L))
                .thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> cashController.getTransactionsByAccountId(1L));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Internal server error"));
        verify(cashService).getTransactionsByAccountId(1L);
    }

    @Test
    void getTransactionsByAccountIdPaged_ValidParameters_ReturnsOk() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<CashTransactionDTO> transactionPage = new PageImpl<>(Arrays.asList(transactionDTO));
        when(cashService.getTransactionsByAccountId(1L, pageable)).thenReturn(transactionPage);

        // When
        ResponseEntity<?> response = cashController.getTransactionsByAccountIdPaged(1L, pageable);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(cashService).getTransactionsByAccountId(1L, pageable);
    }

    @Test
    void getTransactionsByAccountIdAndType_ValidParameters_ReturnsOk() {
        // Given
        List<CashTransactionDTO> transactions = Arrays.asList(transactionDTO);
        when(cashService.getTransactionsByAccountIdAndType(1L, CashTransaction.TransactionType.DEPOSIT))
                .thenReturn(transactions);

        // When
        ResponseEntity<?> response = cashController.getTransactionsByAccountIdAndType(
                1L, CashTransaction.TransactionType.DEPOSIT);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(cashService).getTransactionsByAccountIdAndType(1L, CashTransaction.TransactionType.DEPOSIT);
    }

    @Test
    void getTransactionById_ValidId_ReturnsOk() {
        // Given
        when(cashService.getTransactionById(1L)).thenReturn(Optional.of(transactionDTO));

        // When
        ResponseEntity<?> response = cashController.getTransactionById(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CashTransactionDTO);
        verify(cashService).getTransactionById(1L);
    }

    @Test
    void getTransactionById_InvalidId_ReturnsNotFound() {
        // Given
        when(cashService.getTransactionById(1L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = cashController.getTransactionById(1L);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(cashService).getTransactionById(1L);
    }

    @Test
    void getTotalDepositsByAccountId_ValidAccountId_ReturnsOk() {
        // Given
        BigDecimal totalDeposits = new BigDecimal("500.00");
        when(cashService.getTotalDepositsByAccountId(1L)).thenReturn(totalDeposits);

        // When
        ResponseEntity<?> response = cashController.getTotalDepositsByAccountId(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(totalDeposits, response.getBody());
        verify(cashService).getTotalDepositsByAccountId(1L);
    }

    @Test
    void getTotalWithdrawalsByAccountId_ValidAccountId_ReturnsOk() {
        // Given
        BigDecimal totalWithdrawals = new BigDecimal("200.00");
        when(cashService.getTotalWithdrawalsByAccountId(1L)).thenReturn(totalWithdrawals);

        // When
        ResponseEntity<?> response = cashController.getTotalWithdrawalsByAccountId(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(totalWithdrawals, response.getBody());
        verify(cashService).getTotalWithdrawalsByAccountId(1L);
    }

    @Test
    void getAllTransactions_ReturnsOk() {
        // Given
        List<CashTransactionDTO> transactions = Arrays.asList(transactionDTO);
        when(cashService.getAllTransactions()).thenReturn(transactions);

        // When
        ResponseEntity<?> response = cashController.getAllTransactions();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        verify(cashService).getAllTransactions();
    }
}

