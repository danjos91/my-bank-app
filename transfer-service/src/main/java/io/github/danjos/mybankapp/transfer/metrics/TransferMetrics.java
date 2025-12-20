package io.github.danjos.mybankapp.transfer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransferMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordTransfer(BigDecimal amount, String fromCurrency, String toCurrency, boolean success) {
        Counter.builder("transfer.count")
                .description("Total number of transfers")
                .tag("from_currency", fromCurrency)
                .tag("to_currency", toCurrency)
                .tag("status", success ? "success" : "failed")
                .register(meterRegistry)
                .increment();
        
        meterRegistry.counter("transfer.amount", 
                "from_currency", fromCurrency,
                "to_currency", toCurrency,
                "status", success ? "success" : "failed")
                .increment(amount.doubleValue());
    }
    
    public void recordCurrencyConversion(String fromCurrency, String toCurrency) {
        Counter.builder("transfer.currency_conversion.count")
                .description("Number of currency conversions")
                .tag("from_currency", fromCurrency)
                .tag("to_currency", toCurrency)
                .register(meterRegistry)
                .increment();
    }
    
    public Timer.Sample startTransferTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTransferDuration(Timer.Sample sample, String fromCurrency, String toCurrency, boolean success) {
        sample.stop(Timer.builder("transfer.duration")
                .description("Transfer processing duration")
                .tag("from_currency", fromCurrency)
                .tag("to_currency", toCurrency)
                .tag("status", success ? "success" : "failed")
                .register(meterRegistry));
    }
}

