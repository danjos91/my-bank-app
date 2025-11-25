package io.github.danjos.mybankapp.transfer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers", schema = "transfer_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "from_account_id", nullable = false)
    @NotNull(message = "From account ID is required")
    private Long fromAccountId;
    
    @Column(name = "to_account_id", nullable = false)
    @NotNull(message = "To account ID is required")
    private Long toAccountId;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "from_currency", nullable = false, length = 3)
    @Builder.Default
    private Currency fromCurrency = Currency.RUB;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "to_currency", nullable = false, length = 3)
    @Builder.Default
    private Currency toCurrency = Currency.RUB;
    
    @Column(name = "converted_amount", precision = 19, scale = 2)
    private BigDecimal convertedAmount;
    
    @Column(name = "description", length = 255)
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Business methods
    public boolean isPending() {
        return TransferStatus.PENDING.equals(status);
    }
    
    public boolean isCompleted() {
        return TransferStatus.COMPLETED.equals(status);
    }
    
    public boolean isFailed() {
        return TransferStatus.FAILED.equals(status);
    }
    
    public boolean isCancelled() {
        return TransferStatus.CANCELLED.equals(status);
    }
    
    public void markAsCompleted() {
        this.status = TransferStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markAsFailed() {
        this.status = TransferStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markAsCancelled() {
        this.status = TransferStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }
    
    public String getFormattedAmount() {
        return String.format("%.2f", amount);
    }
    
    public String getFormattedCreatedAt() {
        return createdAt.toString();
    }
    
    public String getFormattedCompletedAt() {
        return completedAt != null ? completedAt.toString() : null;
    }
    
    public enum TransferStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
