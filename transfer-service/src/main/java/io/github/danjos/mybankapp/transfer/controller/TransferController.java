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
        log.info("Creating transfer from account {} to account {}", 
                requestDTO.getFromAccountId(), requestDTO.getToAccountId());
        
        TransferDTO transfer = transferService.createTransfer(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
    }
    
    @GetMapping("/{transferId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferDTO> getTransferById(@PathVariable Long transferId) {
        TransferDTO transfer = transferService.getTransferById(transferId);
        return ResponseEntity.ok(transfer);
    }
    
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TransferDTO>> getTransfersByAccountId(@PathVariable Long accountId) {
        List<TransferDTO> transfers = transferService.getTransfersByAccountId(accountId);
        return ResponseEntity.ok(transfers);
    }
    
    @GetMapping("/account/{accountId}/paged")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransferDTO>> getTransfersByAccountId(@PathVariable Long accountId, Pageable pageable) {
        Page<TransferDTO> transfers = transferService.getTransfersByAccountId(accountId, pageable);
        return ResponseEntity.ok(transfers);
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TransferDTO>> getTransfersByStatus(@PathVariable Transfer.TransferStatus status) {
        List<TransferDTO> transfers = transferService.getTransfersByStatus(status);
        return ResponseEntity.ok(transfers);
    }
    
    @GetMapping("/account/{accountId}/status/{status}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TransferDTO>> getTransfersByAccountIdAndStatus(
            @PathVariable Long accountId, 
            @PathVariable Transfer.TransferStatus status) {
        List<TransferDTO> transfers = transferService.getTransfersByAccountIdAndStatus(accountId, status);
        return ResponseEntity.ok(transfers);
    }
    
    @GetMapping("/account/{accountId}/total-sent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BigDecimal> getTotalSentByAccountId(@PathVariable Long accountId) {
        BigDecimal total = transferService.getTotalSentByAccountId(accountId);
        return ResponseEntity.ok(total);
    }
    
    @GetMapping("/account/{accountId}/total-received")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BigDecimal> getTotalReceivedByAccountId(@PathVariable Long accountId) {
        BigDecimal total = transferService.getTotalReceivedByAccountId(accountId);
        return ResponseEntity.ok(total);
    }
    
    @GetMapping("/account/{accountId}/count/{status}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getTransferCountByAccountIdAndStatus(
            @PathVariable Long accountId, 
            @PathVariable Transfer.TransferStatus status) {
        Long count = transferService.getTransferCountByAccountIdAndStatus(accountId, status);
        return ResponseEntity.ok(count);
    }
    
    @PutMapping("/{transferId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferDTO> cancelTransfer(@PathVariable Long transferId) {
        TransferDTO transfer = transferService.cancelTransfer(transferId);
        return ResponseEntity.ok(transfer);
    }
    
    @PutMapping("/{transferId}/retry")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferDTO> retryFailedTransfer(@PathVariable Long transferId) {
        TransferDTO transfer = transferService.retryFailedTransfer(transferId);
        return ResponseEntity.ok(transfer);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Transfer Service is running");
    }
}
