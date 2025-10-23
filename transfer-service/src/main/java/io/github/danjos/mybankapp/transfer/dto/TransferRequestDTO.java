package io.github.danjos.mybankapp.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDTO {
    
    @NotNull(message = "From account ID is required")
    private Long fromAccountId;
    
    @NotNull(message = "To account ID is required")
    private Long toAccountId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    // Business validation
    public boolean isValidTransfer() {
        return fromAccountId != null && toAccountId != null && 
               !fromAccountId.equals(toAccountId) && 
               amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
