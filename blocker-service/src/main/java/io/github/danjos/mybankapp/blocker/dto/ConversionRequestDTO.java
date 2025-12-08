package io.github.danjos.mybankapp.blocker.dto;

import io.github.danjos.mybankapp.blocker.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionRequestDTO {
    
    @NotNull(message = "From currency is required")
    private Currency fromCurrency;
    
    @NotNull(message = "To currency is required")
    private Currency toCurrency;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
}

