package io.github.danjos.mybankapp.exchange.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates", schema = "exchange_schema",
       uniqueConstraints = @UniqueConstraint(columnNames = {"currency"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, unique = true, length = 3)
    @NotNull(message = "Currency is required")
    private Currency currency;
    
    @Column(name = "buy_rate", nullable = false, precision = 19, scale = 6)
    @NotNull(message = "Buy rate is required")
    @DecimalMin(value = "0.000001", message = "Buy rate must be positive")
    private BigDecimal buyRate;
    
    @Column(name = "sell_rate", nullable = false, precision = 19, scale = 6)
    @NotNull(message = "Sell rate is required")
    @DecimalMin(value = "0.000001", message = "Sell rate must be positive")
    private BigDecimal sellRate;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

