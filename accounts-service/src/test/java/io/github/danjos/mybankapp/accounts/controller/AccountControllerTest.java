package io.github.danjos.mybankapp.accounts.controller;

import io.github.danjos.mybankapp.accounts.dto.AccountDTO;
import io.github.danjos.mybankapp.accounts.entity.Account;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private User testUser;
    private Account testAccount;
    private AccountDTO testAccountDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testAccount = Account.builder()
                .id(1L)
                .user(testUser)
                .balance(BigDecimal.valueOf(1000.00))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testAccountDTO = new AccountDTO(
                1L,
                1L,
                "testuser",
                BigDecimal.valueOf(1000.00),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createAccount_ValidUserId_ReturnsCreatedResponse() {
        // Given
        when(accountService.createAccount(1L)).thenReturn(testAccount);

        // When
        ResponseEntity<?> response = accountController.createAccount(1L);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Account created successfully"));
        verify(accountService).createAccount(1L);
    }

    @Test
    void createAccount_InvalidUserId_ReturnsBadRequest() {
        // Given
        when(accountService.createAccount(1L)).thenThrow(new IllegalArgumentException("User not found"));

        // When
        ResponseEntity<?> response = accountController.createAccount(1L);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User not found", response.getBody());
        verify(accountService).createAccount(1L);
    }

    @Test
    void createAccount_ServiceException_ReturnsInternalServerError() {
        // Given
        when(accountService.createAccount(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.createAccount(1L);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error creating account"));
        verify(accountService).createAccount(1L);
    }

    @Test
    void getAccountsByUserId_ValidUserId_ReturnsAccounts() {
        // Given
        List<Account> accounts = Arrays.asList(testAccount);
        when(accountService.getAccountsByUserId(1L)).thenReturn(accounts);

        // When
        ResponseEntity<?> response = accountController.getAccountsByUserId(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        verify(accountService).getAccountsByUserId(1L);
    }

    @Test
    void getAccountsByUserId_ServiceException_ReturnsInternalServerError() {
        // Given
        when(accountService.getAccountsByUserId(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.getAccountsByUserId(1L);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error retrieving accounts"));
        verify(accountService).getAccountsByUserId(1L);
    }

    @Test
    void getAccountById_ValidAccountId_ReturnsAccount() {
        // Given
        when(accountService.getAccountById(1L)).thenReturn(Optional.of(testAccount));

        // When
        ResponseEntity<?> response = accountController.getAccountById(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof AccountDTO);
        verify(accountService).getAccountById(1L);
    }

    @Test
    void getAccountById_InvalidAccountId_ReturnsNotFound() {
        // Given
        when(accountService.getAccountById(1L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = accountController.getAccountById(1L);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(accountService).getAccountById(1L);
    }

    @Test
    void getAccountById_ServiceException_ReturnsInternalServerError() {
        // Given
        when(accountService.getAccountById(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.getAccountById(1L);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error retrieving account"));
        verify(accountService).getAccountById(1L);
    }

    @Test
    void getBalance_ValidAccountId_ReturnsBalance() {
        // Given
        BigDecimal balance = BigDecimal.valueOf(1000.00);
        when(accountService.getBalance(1L)).thenReturn(balance);

        // When
        ResponseEntity<?> response = accountController.getBalance(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(balance, response.getBody());
        verify(accountService).getBalance(1L);
    }

    @Test
    void getBalance_InvalidAccountId_ReturnsBadRequest() {
        // Given
        when(accountService.getBalance(1L)).thenThrow(new IllegalArgumentException("Account not found"));

        // When
        ResponseEntity<?> response = accountController.getBalance(1L);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Account not found", response.getBody());
        verify(accountService).getBalance(1L);
    }

    @Test
    void getBalance_ServiceException_ReturnsInternalServerError() {
        // Given
        when(accountService.getBalance(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.getBalance(1L);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error retrieving balance"));
        verify(accountService).getBalance(1L);
    }

    @Test
    void getTotalBalanceByUserId_ValidUserId_ReturnsTotalBalance() {
        // Given
        BigDecimal totalBalance = BigDecimal.valueOf(2000.00);
        when(accountService.getTotalBalanceByUserId(1L)).thenReturn(totalBalance);

        // When
        ResponseEntity<?> response = accountController.getTotalBalanceByUserId(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(totalBalance, response.getBody());
        verify(accountService).getTotalBalanceByUserId(1L);
    }

    @Test
    void getTotalBalanceByUserId_ServiceException_ReturnsInternalServerError() {
        // Given
        when(accountService.getTotalBalanceByUserId(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.getTotalBalanceByUserId(1L);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error retrieving total balance"));
        verify(accountService).getTotalBalanceByUserId(1L);
    }

    @Test
    void updateBalance_ValidAccountIdAndBalance_ReturnsSuccess() {
        // Given
        BigDecimal newBalance = BigDecimal.valueOf(1500.00);
        when(accountService.updateBalance(1L, newBalance)).thenReturn(testAccount);

        // When
        ResponseEntity<?> response = accountController.updateBalance(1L, newBalance);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Balance updated successfully"));
        verify(accountService).updateBalance(1L, newBalance);
    }

    @Test
    void updateBalance_InvalidAccountId_ReturnsBadRequest() {
        // Given
        BigDecimal newBalance = BigDecimal.valueOf(1500.00);
        when(accountService.updateBalance(1L, newBalance)).thenThrow(new IllegalArgumentException("Account not found"));

        // When
        ResponseEntity<?> response = accountController.updateBalance(1L, newBalance);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Account not found", response.getBody());
        verify(accountService).updateBalance(1L, newBalance);
    }

    @Test
    void updateBalance_ServiceException_ReturnsInternalServerError() {
        // Given
        BigDecimal newBalance = BigDecimal.valueOf(1500.00);
        when(accountService.updateBalance(1L, newBalance)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.updateBalance(1L, newBalance);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error updating balance"));
        verify(accountService).updateBalance(1L, newBalance);
    }

    @Test
    void addToBalance_ValidAccountIdAndAmount_ReturnsSuccess() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(500.00);
        when(accountService.addToBalance(1L, amount)).thenReturn(testAccount);

        // When
        ResponseEntity<?> response = accountController.addToBalance(1L, amount);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Amount added successfully"));
        verify(accountService).addToBalance(1L, amount);
    }

    @Test
    void addToBalance_InvalidAccountId_ReturnsBadRequest() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(500.00);
        when(accountService.addToBalance(1L, amount)).thenThrow(new IllegalArgumentException("Account not found"));

        // When
        ResponseEntity<?> response = accountController.addToBalance(1L, amount);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Account not found", response.getBody());
        verify(accountService).addToBalance(1L, amount);
    }

    @Test
    void addToBalance_ServiceException_ReturnsInternalServerError() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(500.00);
        when(accountService.addToBalance(1L, amount)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.addToBalance(1L, amount);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error adding to balance"));
        verify(accountService).addToBalance(1L, amount);
    }

    @Test
    void subtractFromBalance_ValidAccountIdAndAmount_ReturnsSuccess() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(200.00);
        when(accountService.subtractFromBalance(1L, amount)).thenReturn(testAccount);

        // When
        ResponseEntity<?> response = accountController.subtractFromBalance(1L, amount);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Amount subtracted successfully"));
        verify(accountService).subtractFromBalance(1L, amount);
    }

    @Test
    void subtractFromBalance_InvalidAccountId_ReturnsBadRequest() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(200.00);
        when(accountService.subtractFromBalance(1L, amount)).thenThrow(new IllegalArgumentException("Account not found"));

        // When
        ResponseEntity<?> response = accountController.subtractFromBalance(1L, amount);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Account not found", response.getBody());
        verify(accountService).subtractFromBalance(1L, amount);
    }

    @Test
    void subtractFromBalance_ServiceException_ReturnsInternalServerError() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(200.00);
        when(accountService.subtractFromBalance(1L, amount)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.subtractFromBalance(1L, amount);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error subtracting from balance"));
        verify(accountService).subtractFromBalance(1L, amount);
    }

    @Test
    void deleteAccount_ValidAccountId_ReturnsSuccess() {
        // Given
        doNothing().when(accountService).deleteAccount(1L);

        // When
        ResponseEntity<?> response = accountController.deleteAccount(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Account deleted successfully", response.getBody());
        verify(accountService).deleteAccount(1L);
    }

    @Test
    void deleteAccount_InvalidAccountId_ReturnsBadRequest() {
        // Given
        doThrow(new IllegalArgumentException("Account not found")).when(accountService).deleteAccount(1L);

        // When
        ResponseEntity<?> response = accountController.deleteAccount(1L);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Account not found", response.getBody());
        verify(accountService).deleteAccount(1L);
    }

    @Test
    void deleteAccount_ServiceException_ReturnsInternalServerError() {
        // Given
        doThrow(new RuntimeException("Database error")).when(accountService).deleteAccount(1L);

        // When
        ResponseEntity<?> response = accountController.deleteAccount(1L);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error deleting account"));
        verify(accountService).deleteAccount(1L);
    }

    @Test
    void getAccountsByUsername_ValidUsername_ReturnsAccounts() {
        // Given
        List<AccountDTO> accounts = Arrays.asList(testAccountDTO);
        when(accountService.getAccountsByUsername("testuser")).thenReturn(accounts);

        // When
        ResponseEntity<?> response = accountController.getAccountsByUsername("testuser");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        verify(accountService).getAccountsByUsername("testuser");
    }

    @Test
    void getAccountsByUsername_ServiceException_ReturnsInternalServerError() {
        // Given
        when(accountService.getAccountsByUsername("testuser")).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.getAccountsByUsername("testuser");

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error retrieving accounts"));
        verify(accountService).getAccountsByUsername("testuser");
    }

    @Test
    void getAllAccounts_ReturnsAllAccounts() {
        // Given
        List<AccountDTO> accounts = Arrays.asList(testAccountDTO);
        when(accountService.getAllAccounts()).thenReturn(accounts);

        // When
        ResponseEntity<?> response = accountController.getAllAccounts();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        verify(accountService).getAllAccounts();
    }

    @Test
    void getAllAccounts_ServiceException_ReturnsInternalServerError() {
        // Given
        when(accountService.getAllAccounts()).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = accountController.getAllAccounts();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error retrieving accounts"));
        verify(accountService).getAllAccounts();
    }
}
