package io.github.danjos.mybankapp.accounts.service;

import io.github.danjos.mybankapp.accounts.dto.UserProfileDTO;
import io.github.danjos.mybankapp.accounts.dto.UserRegistrationDTO;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User registerUser(UserRegistrationDTO registrationDTO) {
        // Validate age
        if (!registrationDTO.isAdult()) {
            throw new IllegalArgumentException("User must be at least 18 years old");
        }
        
        // Check if username already exists
        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Check if email already exists
        if (registrationDTO.getEmail() != null && userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setEmail(registrationDTO.getEmail());
        user.setBirthDate(registrationDTO.getBirthDate());
        
        return userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<User> findByNameContaining(String name) {
        return userRepository.findByNameContaining(name);
    }
    
    public User updateUserProfile(Long userId, UserProfileDTO profileDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Validate age
        if (!profileDTO.isAdult()) {
            throw new IllegalArgumentException("User must be at least 18 years old");
        }
        
        // Check if email is being changed and already exists
        if (profileDTO.getEmail() != null && 
            !profileDTO.getEmail().equals(user.getEmail()) && 
            userRepository.existsByEmail(profileDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        user.setFirstName(profileDTO.getFirstName());
        user.setLastName(profileDTO.getLastName());
        user.setEmail(profileDTO.getEmail());
        user.setBirthDate(profileDTO.getBirthDate());
        
        return userRepository.save(user);
    }
    
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if user has any accounts with balance
        double totalBalance = user.getTotalBalance();
        if (totalBalance > 0) {
            throw new IllegalArgumentException("Cannot delete user with existing balance. Please withdraw all funds first.");
        }
        
        userRepository.delete(user);
    }
    
    @Transactional(readOnly = true)
    public List<UserProfileDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToProfileDTO)
                .collect(Collectors.toList());
    }
    
    private UserProfileDTO convertToProfileDTO(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBirthDate(),
                user.getUsername()
        );
    }
}
