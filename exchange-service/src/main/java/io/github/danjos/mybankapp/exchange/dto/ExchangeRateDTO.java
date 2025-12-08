package io.github.danjos.mybankapp.exchange.dto;

import io.github.danjos.mybankapp.exchange.entity.Currency;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateDTO {
    
    private Long id;
    private Currency currency;
    private BigDecimal buyRate;
    private BigDecimal sellRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

