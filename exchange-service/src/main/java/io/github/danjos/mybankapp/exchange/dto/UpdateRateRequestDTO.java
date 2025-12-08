package io.github.danjos.mybankapp.exchange.dto;

import io.github.danjos.mybankapp.exchange.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRateRequestDTO {
    
    @NotNull(message = "Currency is required")
    private Currency currency;
    
    @NotNull(message = "Buy rate is required")
    @DecimalMin(value = "0.000001", message = "Buy rate must be positive")
    private BigDecimal buyRate;
    
    @NotNull(message = "Sell rate is required")
    @DecimalMin(value = "0.000001", message = "Sell rate must be positive")
    private BigDecimal sellRate;
}

