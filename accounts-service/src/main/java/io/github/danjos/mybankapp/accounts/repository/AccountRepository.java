package io.github.danjos.mybankapp.accounts.repository;

import io.github.danjos.mybankapp.accounts.entity.Account;
import io.github.danjos.mybankapp.accounts.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    List<Account> findByUser(User user);
    
    List<Account> findByUserId(Long userId);
    
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId")
    Optional<Account> findPrimaryAccountByUserId(@Param("userId") Long userId);
    
    @Query("SELECT a FROM Account a WHERE a.user.username = :username")
    List<Account> findByUsername(@Param("username") String username);
    
    @Query("SELECT a FROM Account a WHERE a.balance > :minBalance")
    List<Account> findByBalanceGreaterThan(@Param("minBalance") java.math.BigDecimal minBalance);
    
    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.user.id = :userId")
    java.math.BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);
}
