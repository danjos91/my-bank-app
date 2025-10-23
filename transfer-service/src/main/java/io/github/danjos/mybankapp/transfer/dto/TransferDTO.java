package io.github.danjos.mybankapp.transfer.dto;

import io.github.danjos.mybankapp.transfer.entity.Transfer;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferDTO {
    
    private Long id;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private String description;
    private Transfer.TransferStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    
    // Business methods
    public boolean isPending() {
        return Transfer.TransferStatus.PENDING.equals(status);
    }
    
    public boolean isCompleted() {
        return Transfer.TransferStatus.COMPLETED.equals(status);
    }
    
    public boolean isFailed() {
        return Transfer.TransferStatus.FAILED.equals(status);
    }
    
    public boolean isCancelled() {
        return Transfer.TransferStatus.CANCELLED.equals(status);
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
}
