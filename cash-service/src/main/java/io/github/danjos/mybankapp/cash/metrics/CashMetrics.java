package io.github.danjos.mybankapp.cash.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics collector for cash service operations.
 * Tracks deposits, withdrawals, and their success/failure rates.
 */
@Component
public class CashMetrics {

    private final Counter depositSuccessCounter;
    private final Counter depositFailedBlocked;
    private final Counter depositFailedAccountNotFound;
    private final Counter depositFailedUnknownError;
    private final Counter withdrawalSuccessCounter;
    private final Counter withdrawalFailedBlocked;
    private final Counter withdrawalFailedAccountNotFound;
    private final Counter withdrawalFailedInsufficientBalance;
    private final Counter withdrawalFailedUnknownError;
    private final Map<String, DistributionSummary> depositAmountByCurrency;
    private final Map<String, DistributionSummary> withdrawalAmountByCurrency;
    private final MeterRegistry meterRegistry;

    public CashMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.depositAmountByCurrency = new ConcurrentHashMap<>();
        this.withdrawalAmountByCurrency = new ConcurrentHashMap<>();

        // Deposit success counter
        this.depositSuccessCounter = Counter.builder("cash.deposit.success")
                .description("Number of successful deposit operations")
                .tag("operation", "deposit")
                .register(meterRegistry);

        // Deposit failure counters
        this.depositFailedBlocked = Counter.builder("cash.deposit.failed")
                .description("Number of failed deposits due to being blocked")
                .tag("reason", "blocked")
                .register(meterRegistry);

        this.depositFailedAccountNotFound = Counter.builder("cash.deposit.failed")
                .description("Number of failed deposits due to account not found")
                .tag("reason", "account_not_found")
                .register(meterRegistry);

        this.depositFailedUnknownError = Counter.builder("cash.deposit.failed")
                .description("Number of failed deposits due to unknown error")
                .tag("reason", "unknown_error")
                .register(meterRegistry);

        // Withdrawal success counter
        this.withdrawalSuccessCounter = Counter.builder("cash.withdrawal.success")
                .description("Number of successful withdrawal operations")
                .tag("operation", "withdrawal")
                .register(meterRegistry);

        // Withdrawal failure counters
        this.withdrawalFailedBlocked = Counter.builder("cash.withdrawal.failed")
                .description("Number of failed withdrawals due to being blocked")
                .tag("reason", "blocked")
                .register(meterRegistry);

        this.withdrawalFailedAccountNotFound = Counter.builder("cash.withdrawal.failed")
                .description("Number of failed withdrawals due to account not found")
                .tag("reason", "account_not_found")
                .register(meterRegistry);

        this.withdrawalFailedInsufficientBalance = Counter.builder("cash.withdrawal.failed")
                .description("Number of failed withdrawals due to insufficient balance")
                .tag("reason", "insufficient_balance")
                .register(meterRegistry);

        this.withdrawalFailedUnknownError = Counter.builder("cash.withdrawal.failed")
                .description("Number of failed withdrawals due to unknown error")
                .tag("reason", "unknown_error")
                .register(meterRegistry);
    }

    /**
     * Record a successful deposit.
     */
    public void recordDepositSuccess(BigDecimal amount, String currency) {
        depositSuccessCounter.increment();
        recordDepositAmount(amount, currency);
    }

    /**
     * Record a failed deposit due to being blocked.
     */
    public void recordDepositFailedBlocked() {
        depositFailedBlocked.increment();
    }

    /**
     * Record a failed deposit due to account not found.
     */
    public void recordDepositFailedAccountNotFound() {
        depositFailedAccountNotFound.increment();
    }

    /**
     * Record a failed deposit due to unknown error.
     */
    public void recordDepositFailedUnknownError() {
        depositFailedUnknownError.increment();
    }

    /**
     * Record a successful withdrawal.
     */
    public void recordWithdrawalSuccess(BigDecimal amount, String currency) {
        withdrawalSuccessCounter.increment();
        recordWithdrawalAmount(amount, currency);
    }

    /**
     * Record a failed withdrawal due to being blocked.
     */
    public void recordWithdrawalFailedBlocked() {
        withdrawalFailedBlocked.increment();
    }

    /**
     * Record a failed withdrawal due to account not found.
     */
    public void recordWithdrawalFailedAccountNotFound() {
        withdrawalFailedAccountNotFound.increment();
    }

    /**
     * Record a failed withdrawal due to insufficient balance.
     */
    public void recordWithdrawalFailedInsufficientBalance() {
        withdrawalFailedInsufficientBalance.increment();
    }

    /**
     * Record a failed withdrawal due to unknown error.
     */
    public void recordWithdrawalFailedUnknownError() {
        withdrawalFailedUnknownError.increment();
    }

    /**
     * Record deposit amount by currency.
     */
    private void recordDepositAmount(BigDecimal amount, String currency) {
        String currencyCode = (currency != null && !currency.isEmpty()) ? currency : "RUB";
        
        DistributionSummary summary = depositAmountByCurrency.computeIfAbsent(currencyCode, 
            curr -> DistributionSummary.builder("cash.deposit.amount")
                .description("Distribution of deposit amounts by currency")
                .tag("currency", curr)
                .baseUnit(curr)
                .register(meterRegistry));
        
        summary.record(amount.doubleValue());
    }

    /**
     * Record withdrawal amount by currency.
     */
    private void recordWithdrawalAmount(BigDecimal amount, String currency) {
        String currencyCode = (currency != null && !currency.isEmpty()) ? currency : "RUB";
        
        DistributionSummary summary = withdrawalAmountByCurrency.computeIfAbsent(currencyCode, 
            curr -> DistributionSummary.builder("cash.withdrawal.amount")
                .description("Distribution of withdrawal amounts by currency")
                .tag("currency", curr)
                .baseUnit(curr)
                .register(meterRegistry));
        
        summary.record(amount.doubleValue());
    }
}

