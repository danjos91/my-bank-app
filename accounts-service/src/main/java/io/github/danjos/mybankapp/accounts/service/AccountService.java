package io.github.danjos.mybankapp.accounts.service;

import io.github.danjos.mybankapp.accounts.dto.AccountDTO;
import io.github.danjos.mybankapp.accounts.entity.Account;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.repository.AccountRepository;
import io.github.danjos.mybankapp.accounts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Account createAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Account account = Account.builder()
                .user(user)
                .build();
        return accountRepository.save(account);
    }
    
    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Account> getAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Account> getPrimaryAccountByUserId(Long userId) {
        return accountRepository.findPrimaryAccountByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return account.getBalance();
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceByUserId(Long userId) {
        return accountRepository.getTotalBalanceByUserId(userId);
    }
    
    public Account updateBalance(Long accountId, BigDecimal newBalance) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        
        account.setBalance(newBalance);
        return accountRepository.save(account);
    }
    
    public Account addToBalance(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        account.addToBalance(amount);
        return accountRepository.save(account);
    }
    
    public Account subtractFromBalance(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        account.subtractFromBalance(amount);
        return accountRepository.save(account);
    }
    
    public void deleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        if (!account.canBeDeleted()) {
            throw new IllegalArgumentException("Cannot delete account with existing balance");
        }
        
        accountRepository.delete(account);
    }
    
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AccountDTO> getAccountsByUsername(String username) {
        return accountRepository.findByUsername(username).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private AccountDTO convertToDTO(Account account) {
        return new AccountDTO(
                account.getId(),
                account.getUser().getId(),
                account.getUser().getUsername(),
                account.getBalance(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
