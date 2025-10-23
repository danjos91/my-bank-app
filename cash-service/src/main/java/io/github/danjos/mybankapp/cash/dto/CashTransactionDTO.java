package io.github.danjos.mybankapp.cash.dto;

import io.github.danjos.mybankapp.cash.entity.CashTransaction;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashTransactionDTO {
    
    private Long id;
    private Long accountId;
    private BigDecimal amount;
    private CashTransaction.TransactionType transactionType;
    private String description;
    private LocalDateTime timestamp;
    
    // Business methods
    public boolean isDeposit() {
        return CashTransaction.TransactionType.DEPOSIT.equals(transactionType);
    }
    
    public boolean isWithdrawal() {
        return CashTransaction.TransactionType.WITHDRAWAL.equals(transactionType);
    }
    
    public String getFormattedAmount() {
        return String.format("%.2f", amount);
    }
    
    public String getFormattedTimestamp() {
        return timestamp.toString();
    }
}
