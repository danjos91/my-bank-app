package io.github.danjos.mybankapp.cash.repository;

import io.github.danjos.mybankapp.cash.entity.CashTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {
    
    List<CashTransaction> findByAccountIdOrderByTimestampDesc(Long accountId);
    
    Page<CashTransaction> findByAccountIdOrderByTimestampDesc(Long accountId, Pageable pageable);
    
    List<CashTransaction> findByAccountIdAndTransactionTypeOrderByTimestampDesc(Long accountId, CashTransaction.TransactionType transactionType);
    
    @Query("SELECT ct FROM CashTransaction ct WHERE ct.accountId = :accountId AND ct.timestamp >= :since ORDER BY ct.timestamp DESC")
    List<CashTransaction> findByAccountIdAndTimestampAfterOrderByTimestampDesc(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);
    
    @Query("SELECT SUM(ct.amount) FROM CashTransaction ct WHERE ct.accountId = :accountId AND ct.transactionType = :transactionType")
    BigDecimal getTotalAmountByAccountIdAndTransactionType(@Param("accountId") Long accountId, @Param("transactionType") CashTransaction.TransactionType transactionType);
    
    @Query("SELECT COUNT(ct) FROM CashTransaction ct WHERE ct.accountId = :accountId AND ct.transactionType = :transactionType")
    Long countByAccountIdAndTransactionType(@Param("accountId") Long accountId, @Param("transactionType") CashTransaction.TransactionType transactionType);
    
    @Query("SELECT ct FROM CashTransaction ct WHERE ct.accountId = :accountId AND ct.timestamp BETWEEN :startDate AND :endDate ORDER BY ct.timestamp DESC")
    List<CashTransaction> findByAccountIdAndTimestampBetweenOrderByTimestampDesc(@Param("accountId") Long accountId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT ct FROM CashTransaction ct WHERE ct.accountId = :accountId AND ct.amount >= :minAmount ORDER BY ct.timestamp DESC")
    List<CashTransaction> findByAccountIdAndAmountGreaterThanEqualOrderByTimestampDesc(@Param("accountId") Long accountId, @Param("minAmount") BigDecimal minAmount);
}
