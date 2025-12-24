package io.github.danjos.mybankapp.transfer.aspects;

import io.github.danjos.mybankapp.transfer.dto.TransferDTO;
import io.github.danjos.mybankapp.transfer.dto.TransferRequestDTO;
import io.github.danjos.mybankapp.transfer.entity.Currency;
import io.github.danjos.mybankapp.transfer.metrics.TransferMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferMetricsAspect {
    
    private final TransferMetrics transferMetrics;
    
    @Around("execution(* io.github.danjos.mybankapp.transfer.service.TransferService.createTransfer(..))")
    public Object recordTransferMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample timer = transferMetrics.startTransferTimer();
        TransferRequestDTO requestDTO = null;
        BigDecimal amount = BigDecimal.ZERO;
        
        // Extract request DTO and amount
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof TransferRequestDTO) {
            requestDTO = (TransferRequestDTO) args[0];
            amount = requestDTO.getAmount() != null ? requestDTO.getAmount() : BigDecimal.ZERO;
        }
        
        try {
            // Execute the method
            Object result = joinPoint.proceed();
            
            // Extract currencies and conversion info from result
            if (result instanceof TransferDTO transferDTO) {
                Currency fromCurrency = transferDTO.getFromCurrency() != null 
                    ? transferDTO.getFromCurrency() 
                    : Currency.RUB;
                Currency toCurrency = transferDTO.getToCurrency() != null 
                    ? transferDTO.getToCurrency() 
                    : Currency.RUB;
                
                // Record currency conversion if it occurred
                if (!fromCurrency.equals(toCurrency)) {
                    transferMetrics.recordCurrencyConversion(fromCurrency.name(), toCurrency.name());
                }
                
                // Record successful transfer metrics
                transferMetrics.recordTransfer(amount, fromCurrency.name(), toCurrency.name(), true);
                transferMetrics.recordTransferDuration(timer, fromCurrency.name(), toCurrency.name(), true);
            }
            
            return result;
            
        } catch (Exception e) {
            // For failures, we don't have currency information easily available
            // Use default RUB currency - this is a limitation of extracting metrics via AOP
            // when the information is determined inside the method
            Currency defaultCurrency = Currency.RUB;
            transferMetrics.recordTransfer(amount, defaultCurrency.name(), defaultCurrency.name(), false);
            transferMetrics.recordTransferDuration(timer, defaultCurrency.name(), defaultCurrency.name(), false);
            
            throw e;
        }
    }
}

