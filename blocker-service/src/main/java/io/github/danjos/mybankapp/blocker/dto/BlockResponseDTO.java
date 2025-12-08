package io.github.danjos.mybankapp.blocker.dto;

import io.github.danjos.mybankapp.blocker.entity.BlockedTransaction;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockResponseDTO {
    
    private Long id;
    private String transactionId;
    private String serviceName;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private BlockedTransaction.Decision decision;
    private LocalDateTime timestamp;
}

