package io.github.danjos.mybankapp.transfer.dto;

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
    private Decision decision;
    private LocalDateTime timestamp;
    
    public enum Decision {
        BLOCKED,
        APPROVED
    }
}

