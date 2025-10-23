package io.github.danjos.mybankapp.accounts.integration;

import io.github.danjos.mybankapp.accounts.AccountsServiceApplication;
import io.github.danjos.mybankapp.accounts.entity.Account;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.repository.AccountRepository;
import io.github.danjos.mybankapp.accounts.repository.UserRepository;
import io.github.danjos.mybankapp.accounts.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AccountsServiceApplication.class)
@Testcontainers
@ActiveProfiles("test")
@Transactional
class AccountServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldCreateAccountSuccessfully() {
        // When
        Account createdAccount = accountService.createAccount(testUser.getId());

        // Then
        assertThat(createdAccount).isNotNull();
        assertThat(createdAccount.getId()).isNotNull();
        assertThat(createdAccount.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(createdAccount.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(createdAccount.getCreatedAt()).isNotNull();
        assertThat(createdAccount.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindAccountById() {
        // Given
        Account account = accountService.createAccount(testUser.getId());

        // When
        Optional<Account> foundAccount = accountService.getAccountById(account.getId());

        // Then
        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getId()).isEqualTo(account.getId());
        assertThat(foundAccount.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void shouldFindAccountsByUserId() {
        // Given
        Account account1 = accountService.createAccount(testUser.getId());
        Account account2 = accountService.createAccount(testUser.getId());

        // When
        List<Account> accounts = accountService.getAccountsByUserId(testUser.getId());

        // Then
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getId).contains(account1.getId(), account2.getId());
    }

    @Test
    void shouldUpdateAccountBalance() {
        // Given
        Account account = accountService.createAccount(testUser.getId());
        BigDecimal newBalance = new BigDecimal("100.50");

        // When
        Account updatedAccount = accountService.updateAccountBalance(account.getId(), newBalance);

        // Then
        assertThat(updatedAccount.getBalance()).isEqualTo(newBalance);
        assertThat(updatedAccount.getUpdatedAt()).isAfter(account.getUpdatedAt());
    }

    @Test
    void shouldDeleteAccount() {
        // Given
        Account account = accountService.createAccount(testUser.getId());

        // When
        accountService.deleteAccount(account.getId());

        // Then
        Optional<Account> deletedAccount = accountService.getAccountById(account.getId());
        assertThat(deletedAccount).isEmpty();
    }

    @Test
    void shouldHandleNonExistentAccount() {
        // When
        Optional<Account> account = accountService.getAccountById(999L);

        // Then
        assertThat(account).isEmpty();
    }

    @Test
    void shouldHandleNonExistentUser() {
        // When & Then
        assertThat(accountService.createAccount(999L)).isNull();
    }
}
