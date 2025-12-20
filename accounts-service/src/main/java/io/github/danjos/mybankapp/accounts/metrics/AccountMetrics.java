package io.github.danjos.mybankapp.accounts.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class AccountMetrics {
    
    private final MeterRegistry meterRegistry;
    private final AtomicReference<BigDecimal> totalBalance = new AtomicReference<>(BigDecimal.ZERO);
    
    public void recordAccountCreation(String currency) {
        Counter.builder("account.created.count")
                .description("Total number of accounts created")
                .tag("currency", currency)
                .register(meterRegistry)
                .increment();
    }
    
    public void recordAccountBalance(BigDecimal balance, String currency) {
        meterRegistry.gauge("account.balance", 
                java.util.Map.of("currency", currency), 
                balance.doubleValue());
    }
    
    public void updateTotalBalance(BigDecimal total) {
        totalBalance.set(total);
        Gauge.builder("account.total_balance", totalBalance, b -> b.get().doubleValue())
                .description("Total balance across all accounts")
                .register(meterRegistry);
    }
}

