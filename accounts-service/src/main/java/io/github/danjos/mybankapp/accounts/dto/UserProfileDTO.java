package io.github.danjos.mybankapp.accounts.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    
    private Long id;
    
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    @NotNull(message = "Birth date is required")
    private LocalDate birthDate;
    
    private String username; // Read-only field
    
    // Business methods
    public boolean isAdult() {
        return birthDate.isBefore(LocalDate.now().minusYears(18));
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
