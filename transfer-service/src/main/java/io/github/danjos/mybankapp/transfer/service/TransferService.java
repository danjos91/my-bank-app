package io.github.danjos.mybankapp.transfer.service;

import io.github.danjos.mybankapp.transfer.client.AccountsClient;
import io.github.danjos.mybankapp.transfer.client.ExchangeClient;
import io.github.danjos.mybankapp.transfer.kafka.KafkaNotificationProducer;
import io.github.danjos.mybankapp.transfer.dto.AccountDTO;
import io.github.danjos.mybankapp.transfer.dto.ConversionRequestDTO;
import io.github.danjos.mybankapp.transfer.dto.ConversionResponseDTO;
import io.github.danjos.mybankapp.transfer.dto.TransferDTO;
import io.github.danjos.mybankapp.transfer.dto.TransferRequestDTO;
import io.github.danjos.mybankapp.transfer.entity.Currency;
import io.github.danjos.mybankapp.transfer.entity.Transfer;
import io.github.danjos.mybankapp.transfer.metrics.TransferMetrics;
import io.github.danjos.mybankapp.transfer.repository.TransferRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {
    
    private final TransferRepository transferRepository;
    private final AccountsClient accountsClient;
    private final ExchangeClient exchangeClient;
    private final KafkaNotificationProducer kafkaNotificationProducer;
    private final io.github.danjos.mybankapp.transfer.client.BlockerClient blockerClient;
    private final TransferMetrics transferMetrics;
    
    @Transactional
    @CircuitBreaker(name = "transfer-service", fallbackMethod = "createTransferFallback")
    @Retry(name = "transfer-service")
    public TransferDTO createTransfer(TransferRequestDTO requestDTO) {
        log.info("Creating transfer from account {} to account {} for amount {}", 
                requestDTO.getFromAccountId(), requestDTO.getToAccountId(), requestDTO.getAmount());
        
        // Record transfer attempt
        transferMetrics.recordTransferAttempt();
        
        // Validate transfer request
        if (!requestDTO.isValidTransfer()) {
            throw new IllegalArgumentException("Invalid transfer request");
        }
        
        // Get account information to determine currencies
        AccountDTO fromAccount = accountsClient.getAccount(requestDTO.getFromAccountId());
        AccountDTO toAccount = accountsClient.getAccount(requestDTO.getToAccountId());
        
        if (fromAccount == null) {
            transferMetrics.recordFailedTransferAccountNotFound();
            throw new IllegalArgumentException("From account does not exist");
        }
        
        if (toAccount == null) {
            transferMetrics.recordFailedTransferAccountNotFound();
            throw new IllegalArgumentException("To account does not exist");
        }
        
        // Get currencies from accounts (using same Currency enum)
        Currency fromCurrency = fromAccount.getCurrency() != null ? fromAccount.getCurrency() : Currency.RUB;
        Currency toCurrency = toAccount.getCurrency() != null ? toAccount.getCurrency() : Currency.RUB;
        
        // Check if sender has sufficient balance
        if (fromAccount.getBalance().compareTo(requestDTO.getAmount()) < 0) {
            transferMetrics.recordFailedTransferInsufficientBalance();
            throw new IllegalArgumentException("Insufficient balance for transfer");
        }
        
        // Convert currency if needed
        BigDecimal convertedAmount = requestDTO.getAmount();
        if (!fromCurrency.equals(toCurrency)) {
            log.info("Converting {} {} to {}", requestDTO.getAmount(), fromCurrency, toCurrency);
            ConversionRequestDTO conversionRequest = ConversionRequestDTO.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .amount(requestDTO.getAmount())
                    .build();
            ConversionResponseDTO conversionResponse = exchangeClient.convert(conversionRequest);
            convertedAmount = conversionResponse.getConvertedAmount();
            log.info("Converted {} {} to {} {}", 
                    requestDTO.getAmount(), fromCurrency, convertedAmount, toCurrency);
        }
        
        // Check with blocker service using the from account currency
        try {
            io.github.danjos.mybankapp.transfer.dto.BlockResponseDTO blockResponse = 
                    blockerClient.checkTransaction(requestDTO.getAmount(), fromCurrency.name());
            if (blockResponse != null && blockResponse.getDecision() == 
                    io.github.danjos.mybankapp.transfer.dto.BlockResponseDTO.Decision.BLOCKED) {
                transferMetrics.recordFailedTransferBlocked();
                throw new IllegalArgumentException("Transaction blocked: " + blockResponse.getReason());
            }
        } catch (IllegalArgumentException e) {
            // Re-throw blocked transactions
            throw e;
        } catch (Exception e) {
            log.warn("Blocker service check failed, proceeding with transfer: {}", e.getMessage());
            // Continue with transfer if blocker is unavailable (fallback behavior)
        }
        
        // Create transfer record
        Transfer transfer = Transfer.builder()
                .fromAccountId(requestDTO.getFromAccountId())
                .toAccountId(requestDTO.getToAccountId())
                .amount(requestDTO.getAmount())
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .convertedAmount(convertedAmount)
                .description(requestDTO.getDescription())
                .status(Transfer.TransferStatus.PENDING)
                .build();
        
        try {
            // Execute the transfer
            // Subtract original amount from sender's account
            accountsClient.subtractFromAccountBalance(requestDTO.getFromAccountId(), requestDTO.getAmount());
            // Add converted amount to receiver's account
            accountsClient.addToAccountBalance(requestDTO.getToAccountId(), convertedAmount);
            
            // Mark transfer as completed
            transfer.markAsCompleted();
            transfer = transferRepository.save(transfer);
            
            // Record successful transfer with metrics
            transferMetrics.recordCompletedTransfer(requestDTO.getAmount(), fromCurrency);
            
            // Create notifications
            createTransferNotifications(transfer, fromAccount.getUserId(), toAccount.getUserId());
            
            log.info("Transfer {} completed successfully", transfer.getId());
            return convertToDTO(transfer);
            
        } catch (Exception e) {
            log.error("Error executing transfer: {}", e.getMessage());
            transfer.markAsFailed();
            transfer = transferRepository.save(transfer);
            transferMetrics.recordFailedTransferUnknownError();
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public TransferDTO getTransferById(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new NoSuchElementException("No transfer found with id: " + transferId));
        return convertToDTO(transfer);
    }
    
    @Transactional(readOnly = true)
    public List<TransferDTO> getTransfersByAccountId(Long accountId) {
        List<Transfer> transfers = transferRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId);
        return transfers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<TransferDTO> getTransfersByAccountId(Long accountId, Pageable pageable) {
        Page<Transfer> transfers = transferRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId, pageable);
        return transfers.map(this::convertToDTO);
    }
    
    @Transactional(readOnly = true)
    public List<TransferDTO> getTransfersByStatus(Transfer.TransferStatus status) {
        List<Transfer> transfers = transferRepository.findByStatusOrderByCreatedAtDesc(status);
        return transfers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<TransferDTO> getTransfersByAccountIdAndStatus(Long accountId, Transfer.TransferStatus status) {
        List<Transfer> transfers = transferRepository.findByAccountIdAndStatusOrderByCreatedAtDesc(accountId, status);
        return transfers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalSentByAccountId(Long accountId) {
        BigDecimal total = transferRepository.getTotalSentByAccountId(accountId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalReceivedByAccountId(Long accountId) {
        BigDecimal total = transferRepository.getTotalReceivedByAccountId(accountId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    @Transactional(readOnly = true)
    public Long getTransferCountByAccountIdAndStatus(Long accountId, Transfer.TransferStatus status) {
        return transferRepository.countByFromAccountIdAndStatus(accountId, status) + 
               transferRepository.countByToAccountIdAndStatus(accountId, status);
    }
    
    @Transactional
    public TransferDTO cancelTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new NoSuchElementException("No transfer found with id: " + transferId));
        
        if (!transfer.isPending()) {
            throw new IllegalArgumentException("Only pending transfers can be cancelled");
        }
        
        transfer.markAsCancelled();
        transfer = transferRepository.save(transfer);
        
        log.info("Transfer {} cancelled", transferId);
        return convertToDTO(transfer);
    }
    
    @Transactional
    public TransferDTO retryFailedTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new NoSuchElementException("No transfer found with id: " + transferId + "when retrying failed transfer"));
        
        if (!transfer.isFailed()) {
            throw new IllegalArgumentException("Only failed transfers can be retried");
        }
        
        // Reset status to pending for retry
        transfer.setStatus(Transfer.TransferStatus.PENDING);
        transfer.setCompletedAt(null);
        transfer = transferRepository.save(transfer);
        
        log.info("Transfer {} marked for retry", transferId);
        return convertToDTO(transfer);
    }
    
    private void createTransferNotifications(Transfer transfer, Long fromUserId, Long toUserId) {
        try {
            // Notification for sender
            kafkaNotificationProducer.publishTransferCompletedEvent(
                    fromUserId,
                    transfer.getAmount(),
                    transfer.getFromCurrency().name(),
                    transfer.getToAccountId().toString()
            );
            
            // Notification for receiver
            BigDecimal receivedAmount = transfer.getConvertedAmount() != null 
                    ? transfer.getConvertedAmount() 
                    : transfer.getAmount();
            kafkaNotificationProducer.publishTransferReceivedEvent(
                    toUserId,
                    receivedAmount,
                    transfer.getToCurrency().name(),
                    transfer.getFromAccountId().toString()
            );
            
        } catch (Exception e) {
            log.warn("Failed to create transfer notifications: {}", e.getMessage());
            // Don't fail the transfer if notifications fail
        }
    }
    
    private TransferDTO convertToDTO(Transfer transfer) {
        return TransferDTO.builder()
                .id(transfer.getId())
                .fromAccountId(transfer.getFromAccountId())
                .toAccountId(transfer.getToAccountId())
                .amount(transfer.getAmount())
                .fromCurrency(transfer.getFromCurrency())
                .toCurrency(transfer.getToCurrency())
                .convertedAmount(transfer.getConvertedAmount())
                .description(transfer.getDescription())
                .status(transfer.getStatus())
                .createdAt(transfer.getCreatedAt())
                .updatedAt(transfer.getUpdatedAt())
                .completedAt(transfer.getCompletedAt())
                .build();
    }
    
    // Fallback method
    public TransferDTO createTransferFallback(TransferRequestDTO requestDTO, Exception ex) {
        log.warn("Fallback: Unable to create transfer, returning null");
        throw new RuntimeException("Transfer service is temporarily unavailable");
    }
    
}
