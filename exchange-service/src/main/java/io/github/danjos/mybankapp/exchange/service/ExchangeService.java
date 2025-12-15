package io.github.danjos.mybankapp.exchange.service;

import io.github.danjos.mybankapp.exchange.dto.ConversionRequestDTO;
import io.github.danjos.mybankapp.exchange.dto.ConversionResponseDTO;
import io.github.danjos.mybankapp.exchange.dto.ExchangeRateDTO;
import io.github.danjos.mybankapp.exchange.dto.UpdateRateRequestDTO;
import io.github.danjos.mybankapp.exchange.entity.Currency;
import io.github.danjos.mybankapp.exchange.entity.ExchangeRate;
import io.github.danjos.mybankapp.exchange.metrics.ExchangeMetrics;
import io.github.danjos.mybankapp.exchange.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExchangeService {
    
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeMetrics exchangeMetrics;
    
    @Transactional(readOnly = true)
    public List<ExchangeRateDTO> getAllRates() {
        return exchangeRateRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Optional<ExchangeRateDTO> getRateByCurrency(Currency currency) {
        return exchangeRateRepository.findByCurrency(currency)
                .map(this::convertToDTO);
    }
    
    public ExchangeRateDTO updateRate(UpdateRateRequestDTO request) {
        return exchangeMetrics.getRateUpdateTimer().record(() -> {
            Optional<ExchangeRate> existingRate = exchangeRateRepository.findByCurrency(request.getCurrency());
            
            ExchangeRate rate;
            if (existingRate.isPresent()) {
                rate = existingRate.get();
                rate.setBuyRate(request.getBuyRate());
                rate.setSellRate(request.getSellRate());
            } else {
                rate = ExchangeRate.builder()
                        .currency(request.getCurrency())
                        .buyRate(request.getBuyRate())
                        .sellRate(request.getSellRate())
                        .build();
            }
            
            rate = exchangeRateRepository.save(rate);
            
            // Record successful update
            exchangeMetrics.recordRateUpdate(request.getCurrency());
            
            log.info("Updated exchange rate for {}: buy={}, sell={}", 
                    request.getCurrency(), request.getBuyRate(), request.getSellRate());
            return convertToDTO(rate);
        });
    }
    
    public ConversionResponseDTO convert(ConversionRequestDTO request) {
        // Base currency is RUB, so conversion goes: fromCurrency -> RUB -> toCurrency
        
        Currency fromCurrency = request.getFromCurrency();
        Currency toCurrency = request.getToCurrency();
        BigDecimal amount = request.getAmount();
        
        // If same currency, no conversion needed
        if (fromCurrency == toCurrency) {
            return ConversionResponseDTO.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .originalAmount(amount)
                    .convertedAmount(amount)
                    .exchangeRate(BigDecimal.ONE)
                    .build();
        }
        
        // Convert to RUB first
        BigDecimal amountInRUB;
        ExchangeRate fromRate = null;
        if (fromCurrency == Currency.RUB) {
            amountInRUB = amount;
        } else {
            fromRate = exchangeRateRepository.findByCurrency(fromCurrency)
                    .orElseThrow(() -> new IllegalArgumentException("Exchange rate not found for currency: " + fromCurrency));
            // To convert to RUB, we use buy rate (buying foreign currency with RUB)
            amountInRUB = amount.multiply(fromRate.getBuyRate()).setScale(2, RoundingMode.HALF_UP);
        }
        
        // Convert from RUB to target currency
        BigDecimal convertedAmount;
        BigDecimal exchangeRate;
        if (toCurrency == Currency.RUB) {
            convertedAmount = amountInRUB;
            exchangeRate = BigDecimal.ONE;
        } else {
            ExchangeRate toRate = exchangeRateRepository.findByCurrency(toCurrency)
                    .orElseThrow(() -> new IllegalArgumentException("Exchange rate not found for currency: " + toCurrency));
            // To convert from RUB, we use sell rate (selling foreign currency to get RUB)
            convertedAmount = amountInRUB.divide(toRate.getSellRate(), 2, RoundingMode.HALF_UP);
            // Calculate effective exchange rate
            if (fromCurrency == Currency.RUB) {
                exchangeRate = toRate.getSellRate();
            } else {
                exchangeRate = Objects.requireNonNull(fromRate, "fromRate should not be null when fromCurrency is not RUB")
                        .getBuyRate().divide(toRate.getSellRate(), 6, RoundingMode.HALF_UP);
            }
        }
        
        return ConversionResponseDTO.builder()
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .originalAmount(amount)
                .convertedAmount(convertedAmount)
                .exchangeRate(exchangeRate)
                .build();
    }
    
    private ExchangeRateDTO convertToDTO(ExchangeRate rate) {
        return ExchangeRateDTO.builder()
                .id(rate.getId())
                .currency(rate.getCurrency())
                .buyRate(rate.getBuyRate())
                .sellRate(rate.getSellRate())
                .createdAt(rate.getCreatedAt())
                .updatedAt(rate.getUpdatedAt())
                .build();
    }
}

