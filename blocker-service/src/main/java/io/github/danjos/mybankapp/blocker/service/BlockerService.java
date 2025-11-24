package io.github.danjos.mybankapp.blocker.service;

import io.github.danjos.mybankapp.blocker.dto.BlockRequestDTO;
import io.github.danjos.mybankapp.blocker.dto.BlockResponseDTO;
import io.github.danjos.mybankapp.blocker.entity.BlockedTransaction;
import io.github.danjos.mybankapp.blocker.repository.BlockedTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlockerService {
    
    private final BlockedTransactionRepository repository;
    
    @Value("${blocker.threshold.amount:10000}")
    private BigDecimal thresholdAmount;
    
    @Value("${blocker.threshold.currency:RUB}")
    private String thresholdCurrency;
    
    public BlockResponseDTO checkTransaction(BlockRequestDTO request) {
        log.info("Checking transaction: service={}, amount={}, currency={}", 
                request.getServiceName(), request.getAmount(), request.getCurrency());
        
        // Convert amount to RUB equivalent if needed
        BigDecimal amountInRUB = convertToRUB(request.getAmount(), request.getCurrency());
        
        // Check if amount exceeds threshold
        boolean isBlocked = amountInRUB.compareTo(thresholdAmount) > 0;
        
        BlockedTransaction.Decision decision = isBlocked ? 
                BlockedTransaction.Decision.BLOCKED : 
                BlockedTransaction.Decision.APPROVED;
        
        String reason = isBlocked ? 
                String.format("Amount %s %s (equivalent to %s RUB) exceeds threshold of %s %s", 
                        request.getAmount(), 
                        request.getCurrency() != null ? request.getCurrency() : thresholdCurrency,
                        amountInRUB,
                        thresholdAmount,
                        thresholdCurrency) :
                "Transaction approved";
        
        BlockedTransaction transaction = BlockedTransaction.builder()
                .transactionId(request.getTransactionId())
                .serviceName(request.getServiceName())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .reason(reason)
                .decision(decision)
                .build();
        
        transaction = repository.save(transaction);
        
        log.info("Transaction decision: {} - {}", decision, reason);
        
        return convertToDTO(transaction);
    }
    
    private BigDecimal convertToRUB(BigDecimal amount, String currency) {
        if (currency == null || currency.equals("RUB") || currency.equals(thresholdCurrency)) {
            return amount;
        }
        
        // Simple conversion rates (in production, this would use Exchange Service)
        // For now, using approximate rates
        if ("USD".equals(currency)) {
            return amount.multiply(BigDecimal.valueOf(100)); // 1 USD = 100 RUB
        } else if ("CNY".equals(currency)) {
            return amount.multiply(BigDecimal.valueOf(14)); // 1 CNY = 14 RUB
        }
        
        // Unknown currency, assume it's RUB equivalent
        return amount;
    }
    
    @Transactional(readOnly = true)
    public List<BlockResponseDTO> getBlockedTransactions() {
        return repository.findByDecisionOrderByTimestampDesc(BlockedTransaction.Decision.BLOCKED).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<BlockResponseDTO> getTransactionsByService(String serviceName) {
        return repository.findByServiceNameOrderByTimestampDesc(serviceName).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private BlockResponseDTO convertToDTO(BlockedTransaction transaction) {
        return BlockResponseDTO.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .serviceName(transaction.getServiceName())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .reason(transaction.getReason())
                .decision(transaction.getDecision())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}

