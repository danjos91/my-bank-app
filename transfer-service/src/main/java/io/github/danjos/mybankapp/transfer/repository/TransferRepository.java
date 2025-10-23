package io.github.danjos.mybankapp.transfer.repository;

import io.github.danjos.mybankapp.transfer.entity.Transfer;
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
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    
    List<Transfer> findByFromAccountIdOrderByCreatedAtDesc(Long fromAccountId);
    
    List<Transfer> findByToAccountIdOrderByCreatedAtDesc(Long toAccountId);
    
    List<Transfer> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(Long fromAccountId, Long toAccountId);
    
    Page<Transfer> findByFromAccountIdOrderByCreatedAtDesc(Long fromAccountId, Pageable pageable);
    
    Page<Transfer> findByToAccountIdOrderByCreatedAtDesc(Long toAccountId, Pageable pageable);
    
    List<Transfer> findByStatusOrderByCreatedAtDesc(Transfer.TransferStatus status);
    
    List<Transfer> findByFromAccountIdAndStatusOrderByCreatedAtDesc(Long fromAccountId, Transfer.TransferStatus status);
    
    List<Transfer> findByToAccountIdAndStatusOrderByCreatedAtDesc(Long toAccountId, Transfer.TransferStatus status);
    
    @Query("SELECT t FROM Transfer t WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) AND t.status = :status ORDER BY t.createdAt DESC")
    List<Transfer> findByAccountIdAndStatusOrderByCreatedAtDesc(@Param("accountId") Long accountId, @Param("status") Transfer.TransferStatus status);
    
    @Query("SELECT t FROM Transfer t WHERE t.fromAccountId = :accountId AND t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<Transfer> findByFromAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);
    
    @Query("SELECT t FROM Transfer t WHERE t.toAccountId = :accountId AND t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<Transfer> findByToAccountIdAndCreatedAtAfterOrderByCreatedAtDesc(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);
    
    @Query("SELECT SUM(t.amount) FROM Transfer t WHERE t.fromAccountId = :accountId AND t.status = 'COMPLETED'")
    BigDecimal getTotalSentByAccountId(@Param("accountId") Long accountId);
    
    @Query("SELECT SUM(t.amount) FROM Transfer t WHERE t.toAccountId = :accountId AND t.status = 'COMPLETED'")
    BigDecimal getTotalReceivedByAccountId(@Param("accountId") Long accountId);
    
    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.fromAccountId = :accountId AND t.status = :status")
    Long countByFromAccountIdAndStatus(@Param("accountId") Long accountId, @Param("status") Transfer.TransferStatus status);
    
    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.toAccountId = :accountId AND t.status = :status")
    Long countByToAccountIdAndStatus(@Param("accountId") Long accountId, @Param("status") Transfer.TransferStatus status);
    
    @Query("SELECT t FROM Transfer t WHERE t.amount >= :minAmount ORDER BY t.createdAt DESC")
    List<Transfer> findByAmountGreaterThanEqualOrderByCreatedAtDesc(@Param("minAmount") BigDecimal minAmount);
    
    @Query("SELECT t FROM Transfer t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transfer> findByCreatedAtBetweenOrderByCreatedAtDesc(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
