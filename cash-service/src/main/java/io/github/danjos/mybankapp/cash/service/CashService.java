package io.github.danjos.mybankapp.cash.service;

import io.github.danjos.mybankapp.cash.client.AccountsClient;
import io.github.danjos.mybankapp.cash.client.NotificationsClient;
import io.github.danjos.mybankapp.cash.dto.CashTransactionDTO;
import io.github.danjos.mybankapp.cash.dto.CreateNotificationDTO;
import io.github.danjos.mybankapp.cash.dto.DepositRequestDTO;
import io.github.danjos.mybankapp.cash.dto.WithdrawalRequestDTO;
import io.github.danjos.mybankapp.cash.entity.CashTransaction;
import io.github.danjos.mybankapp.cash.repository.CashTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CashService {
    
    private final CashTransactionRepository cashTransactionRepository;
    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;
    
    public CashTransactionDTO deposit(DepositRequestDTO depositRequest) {
        log.info("Processing deposit for account {}: {}", depositRequest.getAccountId(), depositRequest.getAmount());
        
        // Create transaction record
        CashTransaction transaction = CashTransaction.builder()
                .accountId(depositRequest.getAccountId())
                .amount(depositRequest.getAmount())
                .transactionType(CashTransaction.TransactionType.DEPOSIT)
                .description(depositRequest.getDescription())
                .build();
        
        CashTransaction savedTransaction = cashTransactionRepository.save(transaction);
        
        try {
            // Update account balance
            accountsClient.addToAccountBalance(depositRequest.getAccountId(), depositRequest.getAmount());
            
            // Send notification
            sendDepositNotification(depositRequest.getAccountId(), depositRequest.getAmount());
            
            log.info("Deposit successful for account {}: {}", depositRequest.getAccountId(), depositRequest.getAmount());
            return convertToDTO(savedTransaction);
            
        } catch (Exception e) {
            log.error("Error processing deposit for account {}: {}", depositRequest.getAccountId(), e.getMessage());
            // In a real scenario, you might want to implement compensation logic here
            throw new RuntimeException("Deposit failed: " + e.getMessage());
        }
    }
    
    public CashTransactionDTO withdraw(WithdrawalRequestDTO withdrawalRequest) {
        log.info("Processing withdrawal for account {}: {}", withdrawalRequest.getAccountId(), withdrawalRequest.getAmount());
        
        // Check account balance first
        BigDecimal currentBalance = accountsClient.getAccountBalance(withdrawalRequest.getAccountId());
        if (currentBalance.compareTo(withdrawalRequest.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + currentBalance + ", Requested: " + withdrawalRequest.getAmount());
        }
        
        // Create transaction record
        CashTransaction transaction = CashTransaction.builder()
                .accountId(withdrawalRequest.getAccountId())
                .amount(withdrawalRequest.getAmount())
                .transactionType(CashTransaction.TransactionType.WITHDRAWAL)
                .description(withdrawalRequest.getDescription())
                .build();
        
        CashTransaction savedTransaction = cashTransactionRepository.save(transaction);
        
        try {
            // Update account balance
            accountsClient.subtractFromAccountBalance(withdrawalRequest.getAccountId(), withdrawalRequest.getAmount());
            
            // Send notification
            sendWithdrawalNotification(withdrawalRequest.getAccountId(), withdrawalRequest.getAmount());
            
            log.info("Withdrawal successful for account {}: {}", withdrawalRequest.getAccountId(), withdrawalRequest.getAmount());
            return convertToDTO(savedTransaction);
            
        } catch (Exception e) {
            log.error("Error processing withdrawal for account {}: {}", withdrawalRequest.getAccountId(), e.getMessage());
            // In a real scenario, you might want to implement compensation logic here
            throw new RuntimeException("Withdrawal failed: " + e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public List<CashTransactionDTO> getTransactionsByAccountId(Long accountId) {
        return cashTransactionRepository.findByAccountIdOrderByTimestampDesc(accountId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<CashTransactionDTO> getTransactionsByAccountId(Long accountId, Pageable pageable) {
        return cashTransactionRepository.findByAccountIdOrderByTimestampDesc(accountId, pageable)
                .map(this::convertToDTO);
    }
    
    @Transactional(readOnly = true)
    public List<CashTransactionDTO> getTransactionsByAccountIdAndType(Long accountId, CashTransaction.TransactionType transactionType) {
        return cashTransactionRepository.findByAccountIdAndTransactionTypeOrderByTimestampDesc(accountId, transactionType)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Optional<CashTransactionDTO> getTransactionById(Long id) {
        return cashTransactionRepository.findById(id)
                .map(this::convertToDTO);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalDepositsByAccountId(Long accountId) {
        return cashTransactionRepository.getTotalAmountByAccountIdAndTransactionType(accountId, CashTransaction.TransactionType.DEPOSIT);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalWithdrawalsByAccountId(Long accountId) {
        return cashTransactionRepository.getTotalAmountByAccountIdAndTransactionType(accountId, CashTransaction.TransactionType.WITHDRAWAL);
    }
    
    @Transactional(readOnly = true)
    public List<CashTransactionDTO> getAllTransactions() {
        return cashTransactionRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private void sendDepositNotification(Long accountId, BigDecimal amount) {
        try {
            CreateNotificationDTO notification = CreateNotificationDTO.builder()
                    .userId(accountId) // In real scenario, you'd get userId from accountId
                    .type("DEPOSIT_SUCCESS")
                    .title("Deposit Successful")
                    .message("Deposit of " + amount + " has been processed successfully")
                    .build();
            
            notificationsClient.createNotification(notification);
        } catch (Exception e) {
            log.warn("Failed to send deposit notification: {}", e.getMessage());
        }
    }
    
    private void sendWithdrawalNotification(Long accountId, BigDecimal amount) {
        try {
            CreateNotificationDTO notification = CreateNotificationDTO.builder()
                    .userId(accountId) // In real scenario, you'd get userId from accountId
                    .type("WITHDRAWAL_SUCCESS")
                    .title("Withdrawal Successful")
                    .message("Withdrawal of " + amount + " has been processed successfully")
                    .build();
            
            notificationsClient.createNotification(notification);
        } catch (Exception e) {
            log.warn("Failed to send withdrawal notification: {}", e.getMessage());
        }
    }
    
    private CashTransactionDTO convertToDTO(CashTransaction transaction) {
        return CashTransactionDTO.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccountId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .description(transaction.getDescription())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}
