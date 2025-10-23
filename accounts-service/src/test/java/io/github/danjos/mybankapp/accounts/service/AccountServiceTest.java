package io.github.danjos.mybankapp.accounts.service;

import io.github.danjos.mybankapp.accounts.dto.AccountDTO;
import io.github.danjos.mybankapp.accounts.entity.Account;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.repository.AccountRepository;
import io.github.danjos.mybankapp.accounts.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private Account testAccount;

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
    }

    @Test
    void createAccount_ValidUserId_ReturnsAccount() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.createAccount(1L);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        verify(userRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_InvalidUserId_ThrowsException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.createAccount(1L));
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAccountsByUserId_ValidUserId_ReturnsAccounts() {
        // Given
        List<Account> accounts = Arrays.asList(testAccount);
        when(accountRepository.findByUserId(1L)).thenReturn(accounts);

        // When
        List<Account> result = accountService.getAccountsByUserId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAccount, result.get(0));
        verify(accountRepository).findByUserId(1L);
    }

    @Test
    void getAccountById_ValidAccountId_ReturnsAccount() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When
        Optional<Account> result = accountService.getAccountById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testAccount, result.get());
        verify(accountRepository).findById(1L);
    }

    @Test
    void getAccountById_InvalidAccountId_ReturnsEmpty() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<Account> result = accountService.getAccountById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(accountRepository).findById(1L);
    }

    @Test
    void getPrimaryAccountByUserId_ValidUserId_ReturnsAccount() {
        // Given
        when(accountRepository.findPrimaryAccountByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When
        Optional<Account> result = accountService.getPrimaryAccountByUserId(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testAccount, result.get());
        verify(accountRepository).findPrimaryAccountByUserId(1L);
    }

    @Test
    void getBalance_ValidAccountId_ReturnsBalance() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When
        BigDecimal result = accountService.getBalance(1L);

        // Then
        assertEquals(BigDecimal.valueOf(1000.00), result);
        verify(accountRepository).findById(1L);
    }

    @Test
    void getBalance_InvalidAccountId_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.getBalance(1L));
        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
    }

    @Test
    void getTotalBalanceByUserId_ValidUserId_ReturnsTotalBalance() {
        // Given
        BigDecimal totalBalance = BigDecimal.valueOf(2000.00);
        when(accountRepository.getTotalBalanceByUserId(1L)).thenReturn(totalBalance);

        // When
        BigDecimal result = accountService.getTotalBalanceByUserId(1L);

        // Then
        assertEquals(totalBalance, result);
        verify(accountRepository).getTotalBalanceByUserId(1L);
    }

    @Test
    void updateBalance_ValidAccountIdAndBalance_ReturnsUpdatedAccount() {
        // Given
        BigDecimal newBalance = BigDecimal.valueOf(1500.00);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.updateBalance(1L, newBalance);

        // Then
        assertNotNull(result);
        verify(accountRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void updateBalance_InvalidAccountId_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.updateBalance(1L, BigDecimal.valueOf(1500.00)));
        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateBalance_NegativeBalance_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.updateBalance(1L, BigDecimal.valueOf(-100.00)));
        assertEquals("Balance cannot be negative", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void addToBalance_ValidAccountIdAndAmount_ReturnsUpdatedAccount() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(500.00);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.addToBalance(1L, amount);

        // Then
        assertNotNull(result);
        verify(accountRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void addToBalance_InvalidAccountId_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.addToBalance(1L, BigDecimal.valueOf(500.00)));
        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void addToBalance_NonPositiveAmount_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.addToBalance(1L, BigDecimal.valueOf(-100.00)));
        assertEquals("Amount must be positive", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void subtractFromBalance_ValidAccountIdAndAmount_ReturnsUpdatedAccount() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(200.00);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.subtractFromBalance(1L, amount);

        // Then
        assertNotNull(result);
        verify(accountRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void subtractFromBalance_InvalidAccountId_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.subtractFromBalance(1L, BigDecimal.valueOf(200.00)));
        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void subtractFromBalance_NonPositiveAmount_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.subtractFromBalance(1L, BigDecimal.valueOf(-100.00)));
        assertEquals("Amount must be positive", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void deleteAccount_ValidAccountIdWithZeroBalance_DeletesAccount() {
        // Given
        testAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When
        accountService.deleteAccount(1L);

        // Then
        verify(accountRepository).findById(1L);
        verify(accountRepository).delete(testAccount);
    }

    @Test
    void deleteAccount_InvalidAccountId_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.deleteAccount(1L));
        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).delete(any(Account.class));
    }

    @Test
    void deleteAccount_AccountWithBalance_ThrowsException() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> accountService.deleteAccount(1L));
        assertEquals("Cannot delete account with existing balance", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).delete(any(Account.class));
    }

    @Test
    void getAllAccounts_ReturnsAllAccounts() {
        // Given
        List<Account> accounts = Arrays.asList(testAccount);
        when(accountRepository.findAll()).thenReturn(accounts);

        // When
        List<AccountDTO> result = accountService.getAllAccounts();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAccount.getId(), result.get(0).getId());
        verify(accountRepository).findAll();
    }

    @Test
    void getAccountsByUsername_ValidUsername_ReturnsAccounts() {
        // Given
        List<Account> accounts = Arrays.asList(testAccount);
        when(accountRepository.findByUsername("testuser")).thenReturn(accounts);

        // When
        List<AccountDTO> result = accountService.getAccountsByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAccount.getId(), result.get(0).getId());
        verify(accountRepository).findByUsername("testuser");
    }
}
