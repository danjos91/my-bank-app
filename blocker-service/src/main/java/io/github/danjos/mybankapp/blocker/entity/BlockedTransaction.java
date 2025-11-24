package io.github.danjos.mybankapp.blocker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_transactions", schema = "blocker_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "service_name", nullable = false, length = 50)
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3)
    private String currency;
    
    @Column(name = "reason", length = 255)
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    @NotNull(message = "Decision is required")
    @Builder.Default
    private Decision decision = Decision.BLOCKED;
    
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    public enum Decision {
        BLOCKED,
        APPROVED
    }
}

