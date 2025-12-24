package io.github.danjos.mybankapp.accounts.aspects;

import io.github.danjos.mybankapp.accounts.entity.Account;
import io.github.danjos.mybankapp.accounts.entity.Currency;
import io.github.danjos.mybankapp.accounts.metrics.AccountMetrics;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AccountMetricsAspect {
    
    private final AccountMetrics accountMetrics;
    
    @Around("execution(* io.github.danjos.mybankapp.accounts.service.AccountService.createAccount(..))")
    public Object recordAccountCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        
        // Extract currency from result or parameters
        if (result instanceof Account account) {
            Currency currency = account.getCurrency();
            if (currency != null) {
                accountMetrics.recordAccountCreation(currency.name());
            }
        } else {
            // Fallback: try to extract from parameters
            Object[] args = joinPoint.getArgs();
            if (args.length >= 2 && args[1] instanceof Currency currency) {
                accountMetrics.recordAccountCreation(currency.name());
            } else if (args.length >= 1) {
                // Default to RUB if no currency specified
                accountMetrics.recordAccountCreation(Currency.RUB.name());
            }
        }
        
        return result;
    }
}

