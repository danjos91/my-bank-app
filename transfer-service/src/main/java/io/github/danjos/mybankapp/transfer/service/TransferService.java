package io.github.danjos.mybankapp.transfer.service;

import io.github.danjos.mybankapp.transfer.client.AccountsClient;
import io.github.danjos.mybankapp.transfer.client.NotificationsClient;
import io.github.danjos.mybankapp.transfer.dto.CreateNotificationDTO;
import io.github.danjos.mybankapp.transfer.dto.TransferDTO;
import io.github.danjos.mybankapp.transfer.dto.TransferRequestDTO;
import io.github.danjos.mybankapp.transfer.entity.Transfer;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {
    
    private final TransferRepository transferRepository;
    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;
    
    @Transactional
    @CircuitBreaker(name = "transfer-service", fallbackMethod = "createTransferFallback")
    @Retry(name = "transfer-service")
    public TransferDTO createTransfer(TransferRequestDTO requestDTO) {
        log.info("Creating transfer from account {} to account {} for amount {}", 
                requestDTO.getFromAccountId(), requestDTO.getToAccountId(), requestDTO.getAmount());
        
        // Validate transfer request
        if (!requestDTO.isValidTransfer()) {
            throw new IllegalArgumentException("Invalid transfer request");
        }
        
        // Validate accounts exist
        if (!accountsClient.validateAccountExists(requestDTO.getFromAccountId())) {
            throw new IllegalArgumentException("From account does not exist");
        }
        
        if (!accountsClient.validateAccountExists(requestDTO.getToAccountId())) {
            throw new IllegalArgumentException("To account does not exist");
        }
        
        // Check if sender has sufficient balance
        BigDecimal currentBalance = accountsClient.getAccountBalance(requestDTO.getFromAccountId());
        if (currentBalance.compareTo(requestDTO.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance for transfer");
        }
        
        // Create transfer record
        Transfer transfer = Transfer.builder()
                .fromAccountId(requestDTO.getFromAccountId())
                .toAccountId(requestDTO.getToAccountId())
                .amount(requestDTO.getAmount())
                .description(requestDTO.getDescription())
                .status(Transfer.TransferStatus.PENDING)
                .build();
        
        try {
            // Execute the transfer
            accountsClient.subtractFromAccountBalance(requestDTO.getFromAccountId(), requestDTO.getAmount());
            accountsClient.addToAccountBalance(requestDTO.getToAccountId(), requestDTO.getAmount());
            
            // Mark transfer as completed
            transfer.markAsCompleted();
            transfer = transferRepository.save(transfer);
            
            // Create notifications
            createTransferNotifications(transfer);
            
            log.info("Transfer {} completed successfully", transfer.getId());
            return convertToDTO(transfer);
            
        } catch (Exception e) {
            log.error("Error executing transfer: {}", e.getMessage());
            transfer.markAsFailed();
            transfer = transferRepository.save(transfer);
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public TransferDTO getTransferById(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));
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
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));
        
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
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));
        
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
    
    private void createTransferNotifications(Transfer transfer) {
        try {
            // Notification for sender
            CreateNotificationDTO senderNotification = CreateNotificationDTO.builder()
                    .userId(transfer.getFromAccountId()) // Assuming accountId maps to userId
                    .type("TRANSFER_SENT")
                    .title("Transfer Sent")
                    .message(String.format("You sent %.2f to account %d", 
                            transfer.getAmount(), transfer.getToAccountId()))
                    .build();
            notificationsClient.createNotification(senderNotification);
            
            // Notification for receiver
            CreateNotificationDTO receiverNotification = CreateNotificationDTO.builder()
                    .userId(transfer.getToAccountId()) // Assuming accountId maps to userId
                    .type("TRANSFER_RECEIVED")
                    .title("Transfer Received")
                    .message(String.format("You received %.2f from account %d", 
                            transfer.getAmount(), transfer.getFromAccountId()))
                    .build();
            notificationsClient.createNotification(receiverNotification);
            
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
                .description(transfer.getDescription())
                .status(transfer.getStatus())
                .createdAt(transfer.getCreatedAt())
                .completedAt(transfer.getCompletedAt())
                .build();
    }
    
    // Fallback method
    public TransferDTO createTransferFallback(TransferRequestDTO requestDTO, Exception ex) {
        log.warn("Fallback: Unable to create transfer, returning null");
        throw new RuntimeException("Transfer service is temporarily unavailable");
    }
}
