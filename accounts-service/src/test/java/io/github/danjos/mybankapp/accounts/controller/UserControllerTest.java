package io.github.danjos.mybankapp.accounts.controller;

import io.github.danjos.mybankapp.accounts.dto.UserProfileDTO;
import io.github.danjos.mybankapp.accounts.dto.UserRegistrationDTO;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private UserRegistrationDTO registrationDTO;
    private UserProfileDTO profileDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setUsername("testuser");
        registrationDTO.setPassword("password");
        registrationDTO.setFirstName("Test");
        registrationDTO.setLastName("User");
        registrationDTO.setEmail("test@example.com");
        registrationDTO.setBirthDate(LocalDate.of(1990, 1, 1));

        profileDTO = new UserProfileDTO();
        profileDTO.setId(1L);
        profileDTO.setFirstName("Test");
        profileDTO.setLastName("User");
        profileDTO.setEmail("test@example.com");
        profileDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        profileDTO.setUsername("testuser");
    }

    @Test
    void registerUser_ValidData_ReturnsCreatedResponse() {
        // Given
        when(userService.registerUser(any(UserRegistrationDTO.class))).thenReturn(testUser);

        // When
        ResponseEntity<?> response = userController.registerUser(registrationDTO);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("User registered successfully"));
        verify(userService).registerUser(registrationDTO);
    }

    @Test
    void registerUser_InvalidData_ReturnsBadRequest() {
        // Given
        when(userService.registerUser(any(UserRegistrationDTO.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        // When
        ResponseEntity<?> response = userController.registerUser(registrationDTO);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Username already exists", response.getBody());
        verify(userService).registerUser(registrationDTO);
    }

    @Test
    void registerUser_ServiceException_ReturnsInternalServerError() {
        // Given
        when(userService.registerUser(any(UserRegistrationDTO.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = userController.registerUser(registrationDTO);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Registration failed"));
        verify(userService).registerUser(registrationDTO);
    }

    @Test
    void getUserById_ValidId_ReturnsUser() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<?> response = userController.getUserById(1L, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof UserProfileDTO);
        verify(userService).findById(1L);
    }

    @Test
    void getUserById_InvalidId_ReturnsNotFound() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = userController.getUserById(1L, authentication);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService).findById(1L);
    }

    @Test
    void getUserById_ServiceException_ReturnsInternalServerError() {
        // Given
        when(userService.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = userController.getUserById(1L, authentication);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error retrieving user"));
        verify(userService).findById(1L);
    }

    @Test
    void getUserByUsername_ValidUsername_ReturnsUser() {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<?> response = userController.getUserByUsername("testuser");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof UserProfileDTO);
        verify(userService).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_InvalidUsername_ReturnsNotFound() {
        // Given
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = userController.getUserByUsername("nonexistent");

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService).findByUsername("nonexistent");
    }

    @Test
    void getUserByUsername_ServiceException_ReturnsInternalServerError() {
        // Given
        when(userService.findByUsername("testuser")).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = userController.getUserByUsername("testuser");

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error retrieving user"));
        verify(userService).findByUsername("testuser");
    }

    @Test
    void updateUserProfile_ValidData_ReturnsSuccess() {
        // Given
        when(userService.updateUserProfile(1L, profileDTO)).thenReturn(testUser);

        // When
        ResponseEntity<?> response = userController.updateUserProfile(1L, profileDTO, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User profile updated successfully", response.getBody());
        verify(userService).updateUserProfile(1L, profileDTO);
    }

    @Test
    void updateUserProfile_InvalidData_ReturnsBadRequest() {
        // Given
        when(userService.updateUserProfile(1L, profileDTO))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        // When
        ResponseEntity<?> response = userController.updateUserProfile(1L, profileDTO, authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email already exists", response.getBody());
        verify(userService).updateUserProfile(1L, profileDTO);
    }

    @Test
    void updateUserProfile_ServiceException_ReturnsInternalServerError() {
        // Given
        when(userService.updateUserProfile(1L, profileDTO))
                .thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = userController.updateUserProfile(1L, profileDTO, authentication);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error updating user"));
        verify(userService).updateUserProfile(1L, profileDTO);
    }

    @Test
    void changePassword_ValidData_ReturnsSuccess() {
        // Given
        doNothing().when(userService).changePassword(1L, "newPassword");

        // When
        ResponseEntity<?> response = userController.changePassword(1L, "newPassword", authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password changed successfully", response.getBody());
        verify(userService).changePassword(1L, "newPassword");
    }

    @Test
    void changePassword_InvalidData_ReturnsBadRequest() {
        // Given
        doThrow(new IllegalArgumentException("Password cannot be empty"))
                .when(userService).changePassword(1L, "");

        // When
        ResponseEntity<?> response = userController.changePassword(1L, "", authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Password cannot be empty", response.getBody());
        verify(userService).changePassword(1L, "");
    }

    @Test
    void changePassword_ServiceException_ReturnsInternalServerError() {
        // Given
        doThrow(new RuntimeException("Database error"))
                .when(userService).changePassword(1L, "newPassword");

        // When
        ResponseEntity<?> response = userController.changePassword(1L, "newPassword", authentication);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error changing password"));
        verify(userService).changePassword(1L, "newPassword");
    }

    @Test
    void deleteUser_ValidId_ReturnsSuccess() {
        // Given
        doNothing().when(userService).deleteUser(1L);

        // When
        ResponseEntity<?> response = userController.deleteUser(1L, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User deleted successfully", response.getBody());
        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_InvalidData_ReturnsBadRequest() {
        // Given
        doThrow(new IllegalArgumentException("Cannot delete user with existing balance"))
                .when(userService).deleteUser(1L);

        // When
        ResponseEntity<?> response = userController.deleteUser(1L, authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot delete user with existing balance", response.getBody());
        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_ServiceException_ReturnsInternalServerError() {
        // Given
        doThrow(new RuntimeException("Database error"))
                .when(userService).deleteUser(1L);

        // When
        ResponseEntity<?> response = userController.deleteUser(1L, authentication);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error deleting user"));
        verify(userService).deleteUser(1L);
    }

    @Test
    void searchUsersByName_ValidName_ReturnsUsers() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userService.findByNameContaining("Test")).thenReturn(users);

        // When
        ResponseEntity<?> response = userController.searchUsersByName("Test");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        verify(userService).findByNameContaining("Test");
    }

    @Test
    void searchUsersByName_ServiceException_ReturnsInternalServerError() {
        // Given
        when(userService.findByNameContaining("Test")).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = userController.searchUsersByName("Test");

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error searching users"));
        verify(userService).findByNameContaining("Test");
    }

    @Test
    void getAllUsers_ReturnsAllUsers() {
        // Given
        List<UserProfileDTO> users = Arrays.asList(profileDTO);
        when(userService.getAllUsers()).thenReturn(users);

        // When
        ResponseEntity<?> response = userController.getAllUsers();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        verify(userService).getAllUsers();
    }

    @Test
    void getAllUsers_ServiceException_ReturnsInternalServerError() {
        // Given
        when(userService.getAllUsers()).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = userController.getAllUsers();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error retrieving users"));
        verify(userService).getAllUsers();
    }
}
