package io.github.danjos.mybankapp.transfer.dto;

import io.github.danjos.mybankapp.transfer.entity.Currency;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {
    
    private Long id;
    private Long userId;
    private String username;
    private Currency currency;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Business methods
    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
    
    public String getFormattedBalance() {
        return String.format("%.2f", balance);
    }
}

