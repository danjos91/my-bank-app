package io.github.danjos.mybankapp.exchange.controller;

import io.github.danjos.mybankapp.exchange.dto.ConversionRequestDTO;
import io.github.danjos.mybankapp.exchange.dto.ConversionResponseDTO;
import io.github.danjos.mybankapp.exchange.dto.ExchangeRateDTO;
import io.github.danjos.mybankapp.exchange.dto.UpdateRateRequestDTO;
import io.github.danjos.mybankapp.exchange.entity.Currency;
import io.github.danjos.mybankapp.exchange.service.ExchangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
@Slf4j
public class ExchangeController {
    
    private final ExchangeService exchangeService;
    
    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRateDTO>> getAllRates() {
        List<ExchangeRateDTO> rates = exchangeService.getAllRates();
        return ResponseEntity.ok(rates);
    }
    
    @GetMapping("/rates/{currency}")
    public ResponseEntity<ExchangeRateDTO> getRateByCurrency(@PathVariable Currency currency) {
        Optional<ExchangeRateDTO> rate = exchangeService.getRateByCurrency(currency);
        return rate.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/rates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExchangeRateDTO> updateRate(@Valid @RequestBody UpdateRateRequestDTO request) {
        ExchangeRateDTO rate = exchangeService.updateRate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rate);
    }
    
    @PostMapping("/convert")
    public ResponseEntity<ConversionResponseDTO> convert(@Valid @RequestBody ConversionRequestDTO request) {
        ConversionResponseDTO response = exchangeService.convert(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Exchange Service is running");
    }
}

