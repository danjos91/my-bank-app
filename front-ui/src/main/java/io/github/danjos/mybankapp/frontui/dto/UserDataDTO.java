package io.github.danjos.mybankapp.frontui.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDataDTO {
    
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;
    private Long accountId;
    private BigDecimal balance;
    
    public String getName() {
        return (firstName != null ? firstName : "") + 
               (lastName != null && !lastName.isEmpty() ? " " + lastName : "");
    }
    
    public String getLogin() {
        return username;
    }
    
    public String getBirthDate() {
        return birthDate != null ? birthDate.toString() : "";
    }
}
