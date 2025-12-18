package io.github.danjos.mybankapp.accounts.metrics;

import io.github.danjos.mybankapp.accounts.entity.Currency;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics collector for account operations.
 * Tracks account creation, failures, and balance operations.
 */
@Component
public class AccountMetrics {

    private final Counter accountCreatedCounter;
    private final Counter accountCreationFailedUserNotFound;
    private final Counter accountCreationFailedDuplicate;
    private final Counter accountCreationFailedUnknownError;
    private final Counter balanceAddOperationsCounter;
    private final Counter balanceSubtractOperationsCounter;
    private final Map<Currency, DistributionSummary> balanceAmountByCurrency;
    private final MeterRegistry meterRegistry;

    public AccountMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.balanceAmountByCurrency = new ConcurrentHashMap<>();

        // Account created counter
        this.accountCreatedCounter = Counter.builder("account.created")
                .description("Number of successfully created accounts")
                .tag("type", "success")
                .register(meterRegistry);

        // Account creation failed counters
        this.accountCreationFailedUserNotFound = Counter.builder("account.created.failed")
                .description("Number of failed account creations due to user not found")
                .tag("reason", "user_not_found")
                .register(meterRegistry);

        this.accountCreationFailedDuplicate = Counter.builder("account.created.failed")
                .description("Number of failed account creations due to duplicate account")
                .tag("reason", "duplicate_account")
                .register(meterRegistry);

        this.accountCreationFailedUnknownError = Counter.builder("account.created.failed")
                .description("Number of failed account creations due to unknown error")
                .tag("reason", "unknown_error")
                .register(meterRegistry);

        // Balance operation counters
        this.balanceAddOperationsCounter = Counter.builder("account.balance.operations")
                .description("Number of balance add operations")
                .tag("operation", "add")
                .register(meterRegistry);

        this.balanceSubtractOperationsCounter = Counter.builder("account.balance.operations")
                .description("Number of balance subtract operations")
                .tag("operation", "subtract")
                .register(meterRegistry);
    }

    /**
     * Record a successful account creation.
     */
    public void recordAccountCreated() {
        accountCreatedCounter.increment();
    }

    /**
     * Record a failed account creation due to user not found.
     */
    public void recordAccountCreationFailedUserNotFound() {
        accountCreationFailedUserNotFound.increment();
    }

    /**
     * Record a failed account creation due to duplicate account.
     */
    public void recordAccountCreationFailedDuplicate() {
        accountCreationFailedDuplicate.increment();
    }

    /**
     * Record a failed account creation due to unknown error.
     */
    public void recordAccountCreationFailedUnknownError() {
        accountCreationFailedUnknownError.increment();
    }

    /**
     * Record a balance add operation.
     */
    public void recordBalanceAddOperation(BigDecimal amount, Currency currency) {
        balanceAddOperationsCounter.increment();
        recordBalanceAmount(amount, currency);
    }

    /**
     * Record a balance subtract operation.
     */
    public void recordBalanceSubtractOperation(BigDecimal amount, Currency currency) {
        balanceSubtractOperationsCounter.increment();
        recordBalanceAmount(amount, currency);
    }

    /**
     * Record balance amount by currency.
     */
    private void recordBalanceAmount(BigDecimal amount, Currency currency) {
        String currencyCode = (currency != null) ? currency.name() : "RUB";
        
        DistributionSummary summary = balanceAmountByCurrency.computeIfAbsent(currencyCode, 
            curr -> DistributionSummary.builder("account.balance.amount")
                .description("Distribution of balance change amounts by currency")
                .tag("currency", curr)
                .baseUnit(curr)
                .register(meterRegistry));
        
        summary.record(amount.doubleValue());
    }
}

