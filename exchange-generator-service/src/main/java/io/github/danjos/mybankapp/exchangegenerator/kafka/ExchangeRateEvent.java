package io.github.danjos.mybankapp.exchangegenerator.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Event carrying exchange rate data for a specific currency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String currency;
    private BigDecimal buyRate;
    private BigDecimal sellRate;
    private String sourceService;
    private Long timestamp;
}

