package io.github.danjos.mybankapp.blocker.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics collector for blocker service operations.
 * Tracks blocked and approved transactions, amounts by currency, and threshold violations.
 */
@Component
public class BlockerMetrics {

    private final Counter blockedOperationsCounter;
    private final Counter approvedOperationsCounter;
    private final Counter thresholdViolationsCounter;
    private final Map<String, DistributionSummary> blockedAmountByCurrency;
    private final MeterRegistry meterRegistry;

    public BlockerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.blockedAmountByCurrency = new ConcurrentHashMap<>();

        // Blocked operations counter
        this.blockedOperationsCounter = Counter.builder("blocker.operations.blocked")
                .description("Number of blocked operations")
                .tag("decision", "blocked")
                .register(meterRegistry);

        // Approved operations counter
        this.approvedOperationsCounter = Counter.builder("blocker.operations.approved")
                .description("Number of approved operations")
                .tag("decision", "approved")
                .register(meterRegistry);

        // Threshold violations counter
        this.thresholdViolationsCounter = Counter.builder("blocker.threshold.violations")
                .description("Number of threshold violations detected")
                .tag("type", "security")
                .register(meterRegistry);
    }

    /**
     * Record a blocked operation.
     */
    public void recordBlockedOperation(BigDecimal amount, String currency) {
        blockedOperationsCounter.increment();
        thresholdViolationsCounter.increment();
        recordBlockedAmount(amount, currency);
    }

    /**
     * Record an approved operation.
     */
    public void recordApprovedOperation() {
        approvedOperationsCounter.increment();
    }

    /**
     * Record blocked amount by currency.
     */
    private void recordBlockedAmount(BigDecimal amount, String currency) {
        String currencyCode = (currency != null && !currency.isEmpty()) ? currency : "RUB";
        
        DistributionSummary summary = blockedAmountByCurrency.computeIfAbsent(currencyCode, 
            curr -> DistributionSummary.builder("blocker.blocked.amount")
                .description("Distribution of blocked amounts by currency")
                .tag("currency", curr)
                .baseUnit(curr)
                .register(meterRegistry));
        
        summary.record(amount.doubleValue());
    }
}

