package io.github.danjos.mybankapp.cash.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CashMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordDeposit(BigDecimal amount, String currency) {
        Counter.builder("cash.deposit.count")
                .description("Total number of deposits")
                .tag("currency", currency)
                .register(meterRegistry)
                .increment();
        
        meterRegistry.counter("cash.deposit.amount", "currency", currency)
                .increment(amount.doubleValue());
    }
    
    public void recordWithdrawal(BigDecimal amount, String currency) {
        Counter.builder("cash.withdrawal.count")
                .description("Total number of withdrawals")
                .tag("currency", currency)
                .register(meterRegistry)
                .increment();
        
        meterRegistry.counter("cash.withdrawal.amount", "currency", currency)
                .increment(amount.doubleValue());
    }
    
    public void recordDepositError(String currency, String error) {
        Counter.builder("cash.deposit.errors")
                .description("Number of deposit errors")
                .tag("currency", currency)
                .tag("error", error)
                .register(meterRegistry)
                .increment();
    }
    
    public void recordWithdrawalError(String currency, String error) {
        Counter.builder("cash.withdrawal.errors")
                .description("Number of withdrawal errors")
                .tag("currency", currency)
                .tag("error", error)
                .register(meterRegistry)
                .increment();
    }
    
    public Timer.Sample startDepositTimer() {
        return Timer.start(meterRegistry);
    }
    
    public Timer.Sample startWithdrawalTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordDepositDuration(Timer.Sample sample, String currency) {
        sample.stop(Timer.builder("cash.deposit.duration")
                .description("Deposit processing duration")
                .tag("currency", currency)
                .register(meterRegistry));
    }
    
    public void recordWithdrawalDuration(Timer.Sample sample, String currency) {
        sample.stop(Timer.builder("cash.withdrawal.duration")
                .description("Withdrawal processing duration")
                .tag("currency", currency)
                .register(meterRegistry));
    }
}

