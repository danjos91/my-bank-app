package io.github.danjos.mybankapp.accounts.controller;

import io.github.danjos.mybankapp.accounts.dto.UserProfileDTO;
import io.github.danjos.mybankapp.accounts.dto.UserRegistrationDTO;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        try {
            User user = userService.registerUser(registrationDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("message", "User registered successfully with ID: " + user.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<User> user = userService.findById(id);
            if (user.isPresent()) {
                // Convert to DTO for response
                UserProfileDTO profileDTO = new UserProfileDTO(
                        user.get().getId(),
                        user.get().getFirstName(),
                        user.get().getLastName(),
                        user.get().getEmail(),
                        user.get().getBirthDate(),
                        user.get().getUsername()
                );
                return ResponseEntity.ok(profileDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user: " + e.getMessage());
        }
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            Optional<User> user = userService.findByUsername(username);
            if (user.isPresent()) {
                UserProfileDTO profileDTO = new UserProfileDTO(
                        user.get().getId(),
                        user.get().getFirstName(),
                        user.get().getLastName(),
                        user.get().getEmail(),
                        user.get().getBirthDate(),
                        user.get().getUsername()
                );
                return ResponseEntity.ok(profileDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user: " + e.getMessage());
        }
    }
    
    @PutMapping("/username/{username}/profile")
    public ResponseEntity<?> updateUserProfileByUsername(@PathVariable String username, @RequestBody Map<String, String> profileData, Authentication authentication) {
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            
            // Parse name field (format: "FirstName LastName" or just "FirstName")
            String name = profileData.get("name");
            String firstName = user.getFirstName();
            String lastName = user.getLastName();
            
            if (name != null && !name.trim().isEmpty()) {
                String[] nameParts = name.trim().split("\\s+", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            }
            
            // Parse birthdate
            String birthdateStr = profileData.get("birthdate");
            LocalDate birthDate = user.getBirthDate();
            if (birthdateStr != null && !birthdateStr.trim().isEmpty()) {
                birthDate = LocalDate.parse(birthdateStr);
            }
            
            // Create UserProfileDTO with existing email (front-UI doesn't send email)
            UserProfileDTO profileDTO = new UserProfileDTO(
                    user.getId(),
                    firstName,
                    lastName,
                    user.getEmail(), // Keep existing email
                    birthDate,
                    user.getUsername()
            );
            
            userService.updateUserProfile(user.getId(), profileDTO);
            return ResponseEntity.ok("User profile updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating user: " + e.getMessage());
        }
    }
    
    @PutMapping("/username/{username}/password")
    public ResponseEntity<?> changePasswordByUsername(@PathVariable String username, @RequestBody Map<String, String> passwordData, Authentication authentication) {
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            String newPassword = passwordData.get("password");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password cannot be empty");
            }
            
            userService.changePassword(userOpt.get().getId(), newPassword);
            return ResponseEntity.ok("Password changed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error changing password: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<UserProfileDTO> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving users: " + e.getMessage());
        }
    }
}
