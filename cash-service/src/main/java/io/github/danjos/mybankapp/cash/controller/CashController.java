package io.github.danjos.mybankapp.cash.controller;

import io.github.danjos.mybankapp.cash.dto.CashTransactionDTO;
import io.github.danjos.mybankapp.cash.dto.DepositRequestDTO;
import io.github.danjos.mybankapp.cash.dto.WithdrawalRequestDTO;
import io.github.danjos.mybankapp.cash.entity.CashTransaction;
import io.github.danjos.mybankapp.cash.service.CashService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cash")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CashController {
    
    private final CashService cashService;
    
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@Valid @RequestBody DepositRequestDTO depositRequest) {
        try {
            CashTransactionDTO transaction = cashService.deposit(depositRequest);
            return ResponseEntity.status(201).body(transaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Deposit failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@Valid @RequestBody WithdrawalRequestDTO withdrawalRequest) {
        try {
            CashTransactionDTO transaction = cashService.withdraw(withdrawalRequest);
            return ResponseEntity.status(201).body(transaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Withdrawal failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/account/{accountId}/transactions")
    public ResponseEntity<?> getTransactionsByAccountId(@PathVariable Long accountId) {
        try {
            List<CashTransactionDTO> transactions = cashService.getTransactionsByAccountId(accountId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving transactions: " + e.getMessage());
        }
    }
    
    @GetMapping("/account/{accountId}/transactions/paged")
    public ResponseEntity<?> getTransactionsByAccountIdPaged(@PathVariable Long accountId, Pageable pageable) {
        try {
            Page<CashTransactionDTO> transactions = cashService.getTransactionsByAccountId(accountId, pageable);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving transactions: " + e.getMessage());
        }
    }
    
    @GetMapping("/account/{accountId}/transactions/type/{transactionType}")
    public ResponseEntity<?> getTransactionsByAccountIdAndType(
            @PathVariable Long accountId, 
            @PathVariable CashTransaction.TransactionType transactionType) {
        try {
            List<CashTransactionDTO> transactions = cashService.getTransactionsByAccountIdAndType(accountId, transactionType);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving transactions: " + e.getMessage());
        }
    }
    
    @GetMapping("/transactions/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id) {
        try {
            Optional<CashTransactionDTO> transaction = cashService.getTransactionById(id);
            if (transaction.isPresent()) {
                return ResponseEntity.ok(transaction.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving transaction: " + e.getMessage());
        }
    }
    
    @GetMapping("/account/{accountId}/total-deposits")
    public ResponseEntity<?> getTotalDepositsByAccountId(@PathVariable Long accountId) {
        try {
            BigDecimal totalDeposits = cashService.getTotalDepositsByAccountId(accountId);
            return ResponseEntity.ok(totalDeposits);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving total deposits: " + e.getMessage());
        }
    }
    
    @GetMapping("/account/{accountId}/total-withdrawals")
    public ResponseEntity<?> getTotalWithdrawalsByAccountId(@PathVariable Long accountId) {
        try {
            BigDecimal totalWithdrawals = cashService.getTotalWithdrawalsByAccountId(accountId);
            return ResponseEntity.ok(totalWithdrawals);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving total withdrawals: " + e.getMessage());
        }
    }
    
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions() {
        try {
            List<CashTransactionDTO> transactions = cashService.getAllTransactions();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving transactions: " + e.getMessage());
        }
    }
}
