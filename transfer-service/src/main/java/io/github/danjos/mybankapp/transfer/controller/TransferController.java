package io.github.danjos.mybankapp.transfer.controller;

import io.github.danjos.mybankapp.transfer.dto.TransferDTO;
import io.github.danjos.mybankapp.transfer.dto.TransferRequestDTO;
import io.github.danjos.mybankapp.transfer.entity.Transfer;
import io.github.danjos.mybankapp.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {
    
    private final TransferService transferService;
    
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferDTO> createTransfer(@Valid @RequestBody TransferRequestDTO requestDTO) {
        try {
            log.info("Creating transfer from account {} to account {}", 
                    requestDTO.getFromAccountId(), requestDTO.getToAccountId());
            
            TransferDTO transfer = transferService.createTransfer(requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transfer request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating transfer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{transferId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferDTO> getTransferById(@PathVariable Long transferId) {
        try {
            TransferDTO transfer = transferService.getTransferById(transferId);
            return ResponseEntity.ok(transfer);
        } catch (IllegalArgumentException e) {
            log.warn("Transfer not found: {}", transferId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting transfer {}: {}", transferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TransferDTO>> getTransfersByAccountId(@PathVariable Long accountId) {
        try {
            List<TransferDTO> transfers = transferService.getTransfersByAccountId(accountId);
            return ResponseEntity.ok(transfers);
        } catch (Exception e) {
            log.error("Error getting transfers for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/account/{accountId}/paged")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransferDTO>> getTransfersByAccountId(@PathVariable Long accountId, Pageable pageable) {
        try {
            Page<TransferDTO> transfers = transferService.getTransfersByAccountId(accountId, pageable);
            return ResponseEntity.ok(transfers);
        } catch (Exception e) {
            log.error("Error getting paged transfers for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TransferDTO>> getTransfersByStatus(@PathVariable Transfer.TransferStatus status) {
        try {
            List<TransferDTO> transfers = transferService.getTransfersByStatus(status);
            return ResponseEntity.ok(transfers);
        } catch (Exception e) {
            log.error("Error getting transfers by status {}: {}", status, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/account/{accountId}/status/{status}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TransferDTO>> getTransfersByAccountIdAndStatus(
            @PathVariable Long accountId, 
            @PathVariable Transfer.TransferStatus status) {
        try {
            List<TransferDTO> transfers = transferService.getTransfersByAccountIdAndStatus(accountId, status);
            return ResponseEntity.ok(transfers);
        } catch (Exception e) {
            log.error("Error getting transfers for account {} with status {}: {}", accountId, status, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/account/{accountId}/total-sent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BigDecimal> getTotalSentByAccountId(@PathVariable Long accountId) {
        try {
            BigDecimal total = transferService.getTotalSentByAccountId(accountId);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            log.error("Error getting total sent for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/account/{accountId}/total-received")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BigDecimal> getTotalReceivedByAccountId(@PathVariable Long accountId) {
        try {
            BigDecimal total = transferService.getTotalReceivedByAccountId(accountId);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            log.error("Error getting total received for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/account/{accountId}/count/{status}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getTransferCountByAccountIdAndStatus(
            @PathVariable Long accountId, 
            @PathVariable Transfer.TransferStatus status) {
        try {
            Long count = transferService.getTransferCountByAccountIdAndStatus(accountId, status);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting transfer count for account {} with status {}: {}", accountId, status, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{transferId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferDTO> cancelTransfer(@PathVariable Long transferId) {
        try {
            TransferDTO transfer = transferService.cancelTransfer(transferId);
            return ResponseEntity.ok(transfer);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot cancel transfer {}: {}", transferId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error cancelling transfer {}: {}", transferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{transferId}/retry")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferDTO> retryFailedTransfer(@PathVariable Long transferId) {
        try {
            TransferDTO transfer = transferService.retryFailedTransfer(transferId);
            return ResponseEntity.ok(transfer);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot retry transfer {}: {}", transferId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error retrying transfer {}: {}", transferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Transfer Service is running");
    }
}
