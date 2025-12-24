package io.github.danjos.mybankapp.accounts.metrics;

import io.github.danjos.mybankapp.accounts.entity.Currency;
import io.github.danjos.mybankapp.accounts.repository.AccountRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AccountMetrics {
    
    private final MeterRegistry meterRegistry;
    private final AccountRepository accountRepository;
    
    @PostConstruct
    public void initGauges() {
        // Register gauge for total balance across all accounts (queries DB on each scrape)
        Gauge.builder("account.total_balance", this, AccountMetrics::getTotalBalanceFromDb)
                .description("Total balance across all accounts")
                .register(meterRegistry);
        
        // Register gauges for total balance by currency (queries DB on each scrape)
        for (Currency currency : Currency.values()) {
            final Currency currencyFinal = currency;
            Gauge.builder("account.total_balance", this, 
                    metrics -> metrics.getTotalBalanceByCurrencyFromDb(currencyFinal))
                    .tag("currency", currency.name())
                    .description("Total balance across all accounts by currency")
                    .register(meterRegistry);
        }
    }
    
    public void recordAccountCreation(String currency) {
        Counter.builder("account.created.count")
                .description("Total number of accounts created")
                .tag("currency", currency)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Gets total balance from database. Called by Gauge on each metric scrape.
     * @return total balance as double value
     */
    private double getTotalBalanceFromDb() {
        try {
            BigDecimal total = accountRepository.getTotalBalance();
            return total != null ? total.doubleValue() : 0.0;
        } catch (Exception e) {
            // Return 0 on error to avoid breaking metrics collection
            return 0.0;
        }
    }
    
    /**
     * Gets total balance by currency from database. Called by Gauge on each metric scrape.
     * @param currency the currency to query
     * @return total balance as double value
     */
    private double getTotalBalanceByCurrencyFromDb(Currency currency) {
        try {
            BigDecimal total = accountRepository.getTotalBalanceByCurrency(currency);
            return total != null ? total.doubleValue() : 0.0;
        } catch (Exception e) {
            // Return 0 on error to avoid breaking metrics collection
            return 0.0;
        }
    }
}

