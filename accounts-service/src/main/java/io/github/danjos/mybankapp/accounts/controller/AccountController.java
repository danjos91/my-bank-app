package io.github.danjos.mybankapp.accounts.controller;

import io.github.danjos.mybankapp.accounts.dto.AccountDTO;
import io.github.danjos.mybankapp.accounts.entity.Account;
import io.github.danjos.mybankapp.accounts.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    @PostMapping("/users/{userId}/accounts")
    public ResponseEntity<?> createAccount(@PathVariable Long userId) {
        Account account = accountService.createAccount(userId);
        return ResponseEntity.status(201).body(java.util.Map.of("message", "Account created successfully with ID: " + account.getId()));
    }
    
    @GetMapping("/users/{userId}/accounts")
    public ResponseEntity<?> getAccountsByUserId(@PathVariable Long userId) {
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        List<AccountDTO> accountDTOs = accounts.stream()
                .map(account -> new AccountDTO(
                        account.getId(),
                        account.getUser().getId(),
                        account.getUser().getUsername(),
                        account.getCurrency(),
                        account.getBalance(),
                        account.getCreatedAt(),
                        account.getUpdatedAt()
                ))
                .toList();
        return ResponseEntity.ok(accountDTOs);
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountById(@PathVariable Long accountId) {
        Optional<Account> account = accountService.getAccountById(accountId);
        if (account.isPresent()) {
            AccountDTO accountDTO = new AccountDTO(
                    account.get().getId(),
                    account.get().getUser().getId(),
                    account.get().getUser().getUsername(),
                    account.get().getCurrency(),
                    account.get().getBalance(),
                    account.get().getCreatedAt(),
                    account.get().getUpdatedAt()
            );
            return ResponseEntity.ok(accountDTO);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long accountId) {
        BigDecimal balance = accountService.getBalance(accountId);
        return ResponseEntity.ok(balance);
    }
    
    @GetMapping("/users/{userId}/total-balance")
    public ResponseEntity<?> getTotalBalanceByUserId(@PathVariable Long userId) {
        BigDecimal totalBalance = accountService.getTotalBalanceByUserId(userId);
        return ResponseEntity.ok(totalBalance);
    }
    
    @PutMapping("/{accountId}/balance")
    public ResponseEntity<?> updateBalance(@PathVariable Long accountId, @RequestBody BigDecimal newBalance) {
        Account account = accountService.updateBalance(accountId, newBalance);
        return ResponseEntity.ok(java.util.Map.of("message", "Balance updated successfully to: " + account.getBalance()));
    }
    
    @PostMapping("/{accountId}/add")
    public ResponseEntity<?> addToBalance(@PathVariable Long accountId, @RequestBody BigDecimal amount) {
        Account account = accountService.addToBalance(accountId, amount);
        return ResponseEntity.ok("Amount added successfully. New balance: " + account.getBalance());
    }
    
    @PostMapping("/{accountId}/subtract")
    public ResponseEntity<?> subtractFromBalance(@PathVariable Long accountId, @RequestBody BigDecimal amount) {
        Account account = accountService.subtractFromBalance(accountId, amount);
        return ResponseEntity.ok("Amount subtracted successfully. New balance: " + account.getBalance());
    }
    
    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.ok("Account deleted successfully");
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getAccountsByUsername(@PathVariable String username) {
        List<AccountDTO> accounts = accountService.getAccountsByUsername(username);
        return ResponseEntity.ok(accounts);
    }
    
    @GetMapping
    public ResponseEntity<?> getAllAccounts() {
        List<AccountDTO> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }
}
