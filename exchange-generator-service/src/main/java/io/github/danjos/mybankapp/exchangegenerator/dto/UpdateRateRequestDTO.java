package io.github.danjos.mybankapp.exchangegenerator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRateRequestDTO {
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("buyRate")
    private BigDecimal buyRate;
    
    @JsonProperty("sellRate")
    private BigDecimal sellRate;
}

