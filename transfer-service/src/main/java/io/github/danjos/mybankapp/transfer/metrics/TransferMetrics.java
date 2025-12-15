package io.github.danjos.mybankapp.transfer.metrics;

import io.github.danjos.mybankapp.transfer.entity.Currency;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics collector for transfer operations.
 * Tracks transfer counts, failures, amounts by currency, and suspicious operations.
 */
@Component
public class TransferMetrics {

    private final Counter totalTransfersCounter;
    private final Counter completedTransfersCounter;
    private final Counter failedTransfersInsufficientBalance;
    private final Counter failedTransfersAccountNotFound;
    private final Counter failedTransfersBlocked;
    private final Counter failedTransfersUnknownError;
    private final Counter suspiciousOperationsCounter;
    private final Map<Currency, DistributionSummary> transferAmountByCurrency;
    private final MeterRegistry meterRegistry;

    public TransferMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.transferAmountByCurrency = new ConcurrentHashMap<>();

        // Total transfer counter
        this.totalTransfersCounter = Counter.builder("transfer.total")
                .description("Total number of transfer attempts")
                .tag("type", "transfer")
                .register(meterRegistry);

        // Completed transfer counter
        this.completedTransfersCounter = Counter.builder("transfer.completed")
                .description("Number of successfully completed transfers")
                .tag("status", "success")
                .register(meterRegistry);

        // Failed transfer counters by reason
        this.failedTransfersInsufficientBalance = Counter.builder("transfer.failed")
                .description("Number of failed transfers due to insufficient balance")
                .tag("reason", "insufficient_balance")
                .register(meterRegistry);

        this.failedTransfersAccountNotFound = Counter.builder("transfer.failed")
                .description("Number of failed transfers due to account not found")
                .tag("reason", "account_not_found")
                .register(meterRegistry);

        this.failedTransfersBlocked = Counter.builder("transfer.failed")
                .description("Number of failed transfers due to being blocked")
                .tag("reason", "blocked")
                .register(meterRegistry);

        this.failedTransfersUnknownError = Counter.builder("transfer.failed")
                .description("Number of failed transfers due to unknown errors")
                .tag("reason", "unknown_error")
                .register(meterRegistry);

        // Suspicious operations counter
        this.suspiciousOperationsCounter = Counter.builder("transfer.suspicious")
                .description("Number of detected suspicious operations")
                .tag("type", "security")
                .register(meterRegistry);
    }

    /**
     * Record a new transfer attempt.
     */
    public void recordTransferAttempt() {
        totalTransfersCounter.increment();
    }

    /**
     * Record a completed transfer with amount and currency.
     */
    public void recordCompletedTransfer(BigDecimal amount, Currency currency) {
        completedTransfersCounter.increment();
        recordTransferAmount(amount, currency);
    }

    /**
     * Record transfer amount by currency.
     */
    public void recordTransferAmount(BigDecimal amount, Currency currency) {
        DistributionSummary summary = transferAmountByCurrency.computeIfAbsent(currency, 
            curr -> DistributionSummary.builder("transfer.amount")
                .description("Distribution of transfer amounts by currency")
                .tag("currency", curr.name())
                .baseUnit(curr.name())
                .register(meterRegistry));
        
        summary.record(amount.doubleValue());
    }

    /**
     * Record a failed transfer due to insufficient balance.
     */
    public void recordFailedTransferInsufficientBalance() {
        failedTransfersInsufficientBalance.increment();
    }

    /**
     * Record a failed transfer due to account not found.
     */
    public void recordFailedTransferAccountNotFound() {
        failedTransfersAccountNotFound.increment();
    }

    /**
     * Record a failed transfer due to being blocked.
     */
    public void recordFailedTransferBlocked() {
        failedTransfersBlocked.increment();
        recordSuspiciousOperation();
    }

    /**
     * Record a failed transfer due to unknown error.
     */
    public void recordFailedTransferUnknownError() {
        failedTransfersUnknownError.increment();
    }

    /**
     * Record a suspicious operation detected.
     */
    public void recordSuspiciousOperation() {
        suspiciousOperationsCounter.increment();
    }
}

