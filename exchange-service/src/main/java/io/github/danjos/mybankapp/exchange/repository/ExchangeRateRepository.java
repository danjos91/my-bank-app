package io.github.danjos.mybankapp.exchange.repository;

import io.github.danjos.mybankapp.exchange.entity.Currency;
import io.github.danjos.mybankapp.exchange.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    
    Optional<ExchangeRate> findByCurrency(Currency currency);
}

