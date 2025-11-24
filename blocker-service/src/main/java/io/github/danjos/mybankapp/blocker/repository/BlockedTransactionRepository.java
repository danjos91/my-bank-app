package io.github.danjos.mybankapp.blocker.repository;

import io.github.danjos.mybankapp.blocker.entity.BlockedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockedTransactionRepository extends JpaRepository<BlockedTransaction, Long> {
    
    List<BlockedTransaction> findByServiceNameOrderByTimestampDesc(String serviceName);
    
    List<BlockedTransaction> findByDecisionOrderByTimestampDesc(BlockedTransaction.Decision decision);
}

