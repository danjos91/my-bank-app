package io.github.danjos.mybankapp.blocker.controller;

import io.github.danjos.mybankapp.blocker.dto.BlockRequestDTO;
import io.github.danjos.mybankapp.blocker.dto.BlockResponseDTO;
import io.github.danjos.mybankapp.blocker.service.BlockerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blocker")
@RequiredArgsConstructor
@Slf4j
public class BlockerController {
    
    private final BlockerService blockerService;
    
    @PostMapping("/check")
    public ResponseEntity<BlockResponseDTO> checkTransaction(@Valid @RequestBody BlockRequestDTO request) {
        log.info("Received block check request: service={}, amount={}", 
                request.getServiceName(), request.getAmount());
        
        BlockResponseDTO response = blockerService.checkTransaction(request);
        
        if (response.getDecision() == io.github.danjos.mybankapp.blocker.entity.BlockedTransaction.Decision.BLOCKED) {
            return ResponseEntity.status(403).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/blocked")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BlockResponseDTO>> getBlockedTransactions() {
        List<BlockResponseDTO> transactions = blockerService.getBlockedTransactions();
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/service/{serviceName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BlockResponseDTO>> getTransactionsByService(@PathVariable String serviceName) {
        List<BlockResponseDTO> transactions = blockerService.getTransactionsByService(serviceName);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Blocker Service is running");
    }
}

