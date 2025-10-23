package io.github.danjos.mybankapp.cash.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_transactions", schema = "cash_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    
    @Column(name = "description", length = 255)
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    // Business methods
    public boolean isDeposit() {
        return TransactionType.DEPOSIT.equals(transactionType);
    }
    
    public boolean isWithdrawal() {
        return TransactionType.WITHDRAWAL.equals(transactionType);
    }
    
    public String getFormattedAmount() {
        return String.format("%.2f", amount);
    }
    
    public String getFormattedTimestamp() {
        return timestamp.toString();
    }
    
    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL
    }
}
