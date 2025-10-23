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
        try {
            Account account = accountService.createAccount(userId);
            return ResponseEntity.status(201).body("Account created successfully with ID: " + account.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating account: " + e.getMessage());
        }
    }
    
    @GetMapping("/users/{userId}/accounts")
    public ResponseEntity<?> getAccountsByUserId(@PathVariable Long userId) {
        try {
            List<Account> accounts = accountService.getAccountsByUserId(userId);
            List<AccountDTO> accountDTOs = accounts.stream()
                    .map(account -> new AccountDTO(
                            account.getId(),
                            account.getUser().getId(),
                            account.getUser().getUsername(),
                            account.getBalance(),
                            account.getCreatedAt(),
                            account.getUpdatedAt()
                    ))
                    .toList();
            return ResponseEntity.ok(accountDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving accounts: " + e.getMessage());
        }
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountById(@PathVariable Long accountId) {
        try {
            Optional<Account> account = accountService.getAccountById(accountId);
            if (account.isPresent()) {
                AccountDTO accountDTO = new AccountDTO(
                        account.get().getId(),
                        account.get().getUser().getId(),
                        account.get().getUser().getUsername(),
                        account.get().getBalance(),
                        account.get().getCreatedAt(),
                        account.get().getUpdatedAt()
                );
                return ResponseEntity.ok(accountDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving account: " + e.getMessage());
        }
    }
    
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long accountId) {
        try {
            BigDecimal balance = accountService.getBalance(accountId);
            return ResponseEntity.ok(balance);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving balance: " + e.getMessage());
        }
    }
    
    @GetMapping("/users/{userId}/total-balance")
    public ResponseEntity<?> getTotalBalanceByUserId(@PathVariable Long userId) {
        try {
            BigDecimal totalBalance = accountService.getTotalBalanceByUserId(userId);
            return ResponseEntity.ok(totalBalance);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving total balance: " + e.getMessage());
        }
    }
    
    @PutMapping("/{accountId}/balance")
    public ResponseEntity<?> updateBalance(@PathVariable Long accountId, @RequestBody BigDecimal newBalance) {
        try {
            Account account = accountService.updateBalance(accountId, newBalance);
            return ResponseEntity.ok("Balance updated successfully to: " + account.getBalance());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating balance: " + e.getMessage());
        }
    }
    
    @PostMapping("/{accountId}/add")
    public ResponseEntity<?> addToBalance(@PathVariable Long accountId, @RequestBody BigDecimal amount) {
        try {
            Account account = accountService.addToBalance(accountId, amount);
            return ResponseEntity.ok("Amount added successfully. New balance: " + account.getBalance());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error adding to balance: " + e.getMessage());
        }
    }
    
    @PostMapping("/{accountId}/subtract")
    public ResponseEntity<?> subtractFromBalance(@PathVariable Long accountId, @RequestBody BigDecimal amount) {
        try {
            Account account = accountService.subtractFromBalance(accountId, amount);
            return ResponseEntity.ok("Amount subtracted successfully. New balance: " + account.getBalance());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error subtracting from balance: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long accountId) {
        try {
            accountService.deleteAccount(accountId);
            return ResponseEntity.ok("Account deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting account: " + e.getMessage());
        }
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getAccountsByUsername(@PathVariable String username) {
        try {
            List<AccountDTO> accounts = accountService.getAccountsByUsername(username);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving accounts: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getAllAccounts() {
        try {
            List<AccountDTO> accounts = accountService.getAllAccounts();
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving accounts: " + e.getMessage());
        }
    }
}
