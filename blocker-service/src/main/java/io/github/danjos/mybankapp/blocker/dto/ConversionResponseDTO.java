package io.github.danjos.mybankapp.blocker.dto;

import io.github.danjos.mybankapp.blocker.entity.Currency;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionResponseDTO {
    
    private Currency fromCurrency;
    private Currency toCurrency;
    private BigDecimal originalAmount;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;
}

