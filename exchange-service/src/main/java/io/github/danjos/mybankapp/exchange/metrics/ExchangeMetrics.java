package io.github.danjos.mybankapp.exchange.metrics;

import io.github.danjos.mybankapp.exchange.entity.Currency;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics collector for exchange service operations.
 * Tracks exchange rate updates, missing events, update lag, and currency availability.
 */
@Component
public class ExchangeMetrics {

    private final Counter rateUpdatesCounter;
    private final Counter missingRateEventsCounter;
    private final Counter invalidRateEventsCounter;
    private final Timer rateUpdateProcessingTime;
    private final Map<Currency, LocalDateTime> lastUpdateTime;
    private final Map<Currency, Gauge> currencyAvailabilityGauges;
    private final MeterRegistry meterRegistry;

    public ExchangeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.lastUpdateTime = new ConcurrentHashMap<>();
        this.currencyAvailabilityGauges = new ConcurrentHashMap<>();

        // Rate updates counter
        this.rateUpdatesCounter = Counter.builder("exchange.rate.updates")
                .description("Number of successful exchange rate updates")
                .tag("type", "update")
                .register(meterRegistry);

        // Missing rate events counter
        this.missingRateEventsCounter = Counter.builder("exchange.rate.missing")
                .description("Number of missing or failed exchange rate events")
                .tag("type", "error")
                .register(meterRegistry);

        // Invalid rate events counter
        this.invalidRateEventsCounter = Counter.builder("exchange.rate.invalid")
                .description("Number of invalid exchange rate events")
                .tag("type", "error")
                .register(meterRegistry);

        // Rate update processing time
        this.rateUpdateProcessingTime = Timer.builder("exchange.rate.processing.time")
                .description("Time taken to process exchange rate updates")
                .register(meterRegistry);

        // Register currency availability gauges for common currencies
        registerCurrencyAvailabilityGauge(Currency.USD);
        registerCurrencyAvailabilityGauge(Currency.CNY);
    }

    /**
     * Record a successful rate update.
     */
    public void recordRateUpdate(Currency currency) {
        rateUpdatesCounter.increment();
        lastUpdateTime.put(currency, LocalDateTime.now());
    }

    /**
     * Record a missing or failed rate event.
     */
    public void recordMissingRateEvent() {
        missingRateEventsCounter.increment();
    }

    /**
     * Record an invalid rate event.
     */
    public void recordInvalidRateEvent() {
        invalidRateEventsCounter.increment();
    }

    /**
     * Get the timer for rate update processing.
     */
    public Timer getRateUpdateTimer() {
        return rateUpdateProcessingTime;
    }

    /**
     * Get update lag in seconds for a currency.
     */
    public long getUpdateLagSeconds(Currency currency) {
        LocalDateTime lastUpdate = lastUpdateTime.get(currency);
        if (lastUpdate == null) {
            return -1; // Never updated
        }
        return Duration.between(lastUpdate, LocalDateTime.now()).getSeconds();
    }

    /**
     * Register a gauge for currency availability (1 if available, 0 if not or stale).
     */
    private void registerCurrencyAvailabilityGauge(Currency currency) {
        Gauge gauge = Gauge.builder("exchange.currency.available", this,
                metrics -> {
                    long lagSeconds = metrics.getUpdateLagSeconds(currency);
                    // Consider available if updated within last 60 seconds, otherwise unavailable
                    return (lagSeconds >= 0 && lagSeconds < 60) ? 1.0 : 0.0;
                })
                .description("Currency pair availability (1=available, 0=unavailable/stale)")
                .tag("currency", currency.name())
                .register(meterRegistry);
        
        currencyAvailabilityGauges.put(currency, gauge);
    }
}

