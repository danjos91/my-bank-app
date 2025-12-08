package io.github.danjos.mybankapp.accounts.controller;

import io.github.danjos.mybankapp.accounts.dto.UserProfileDTO;
import io.github.danjos.mybankapp.accounts.dto.UserRegistrationDTO;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.exception.GlobalExceptionHandler;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;
    
    private GlobalExceptionHandler exceptionHandler;

    private User testUser;
    private UserRegistrationDTO registrationDTO;
    private UserProfileDTO profileDTO;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        
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
    
    private ResponseEntity<?> invokeControllerWithExceptionHandling(java.util.function.Supplier<ResponseEntity<?>> controllerMethod) {
        try {
            return controllerMethod.get();
        } catch (IllegalArgumentException e) {
            return exceptionHandler.handleIllegalArgumentException(e);
        } catch (Exception e) {
            return exceptionHandler.handleException(e);
        }
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
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> userController.registerUser(registrationDTO));

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Username already exists", body.get("error"));
        verify(userService).registerUser(registrationDTO);
    }

    @Test
    void registerUser_ServiceException_ReturnsInternalServerError() {
        // Given
        when(userService.registerUser(any(UserRegistrationDTO.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> userController.registerUser(registrationDTO));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").toString().contains("Internal server error"));
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
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> userController.getUserById(1L, authentication));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").toString().contains("Internal server error"));
        verify(userService).findById(1L);
    }

    @Test
    void updateUserProfileByUsername_WithOnlyFirstName_ReturnsSuccess() {
        // Given
        java.util.Map<String, String> profileData = new java.util.HashMap<>();
        profileData.put("name", "OnlyFirst");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.updateUserProfile(eq(1L), any(UserProfileDTO.class))).thenReturn(testUser);

        // When
        ResponseEntity<?> response = userController.updateUserProfileByUsername("testuser", profileData, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User profile updated successfully", response.getBody());
        verify(userService).findByUsername("testuser");
        verify(userService).updateUserProfile(eq(1L), any(UserProfileDTO.class));
    }

    @Test
    void updateUserProfileByUsername_WithOnlyBirthdate_ReturnsSuccess() {
        // Given
        java.util.Map<String, String> profileData = new java.util.HashMap<>();
        profileData.put("birthdate", "2000-01-01");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.updateUserProfile(eq(1L), any(UserProfileDTO.class))).thenReturn(testUser);

        // When
        ResponseEntity<?> response = userController.updateUserProfileByUsername("testuser", profileData, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User profile updated successfully", response.getBody());
        verify(userService).findByUsername("testuser");
        verify(userService).updateUserProfile(eq(1L), any(UserProfileDTO.class));
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
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> userController.getUserByUsername("testuser"));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").toString().contains("Internal server error"));
        verify(userService).findByUsername("testuser");
    }

    @Test
    void updateUserProfileByUsername_ValidData_ReturnsSuccess() {
        // Given
        java.util.Map<String, String> profileData = new java.util.HashMap<>();
        profileData.put("name", "Updated First Updated Last");
        profileData.put("birthdate", "1995-05-15");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.updateUserProfile(eq(1L), any(UserProfileDTO.class))).thenReturn(testUser);

        // When
        ResponseEntity<?> response = userController.updateUserProfileByUsername("testuser", profileData, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User profile updated successfully", response.getBody());
        verify(userService).findByUsername("testuser");
        verify(userService).updateUserProfile(eq(1L), any(UserProfileDTO.class));
    }

    @Test
    void updateUserProfileByUsername_UserNotFound_ReturnsNotFound() {
        // Given
        java.util.Map<String, String> profileData = new java.util.HashMap<>();
        profileData.put("name", "Updated Name");
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = userController.updateUserProfileByUsername("nonexistent", profileData, authentication);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService).findByUsername("nonexistent");
        verify(userService, never()).updateUserProfile(anyLong(), any(UserProfileDTO.class));
    }

    @Test
    void updateUserProfileByUsername_InvalidData_ReturnsBadRequest() {
        // Given
        java.util.Map<String, String> profileData = new java.util.HashMap<>();
        profileData.put("name", "Updated Name");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.updateUserProfile(eq(1L), any(UserProfileDTO.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> userController.updateUserProfileByUsername("testuser", profileData, authentication));

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Email already exists", body.get("error"));
        verify(userService).findByUsername("testuser");
        verify(userService).updateUserProfile(eq(1L), any(UserProfileDTO.class));
    }

    @Test
    void updateUserProfileByUsername_ServiceException_ReturnsInternalServerError() {
        // Given
        java.util.Map<String, String> profileData = new java.util.HashMap<>();
        profileData.put("name", "Updated Name");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.updateUserProfile(eq(1L), any(UserProfileDTO.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> userController.updateUserProfileByUsername("testuser", profileData, authentication));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").toString().contains("Internal server error"));
        verify(userService).findByUsername("testuser");
        verify(userService).updateUserProfile(eq(1L), any(UserProfileDTO.class));
    }

    @Test
    void changePasswordByUsername_ValidData_ReturnsSuccess() {
        // Given
        java.util.Map<String, String> passwordData = new java.util.HashMap<>();
        passwordData.put("password", "newPassword");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doNothing().when(userService).changePassword(1L, "newPassword");

        // When
        ResponseEntity<?> response = userController.changePasswordByUsername("testuser", passwordData, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password changed successfully", response.getBody());
        verify(userService).findByUsername("testuser");
        verify(userService).changePassword(1L, "newPassword");
    }

    @Test
    void changePasswordByUsername_UserNotFound_ReturnsNotFound() {
        // Given
        java.util.Map<String, String> passwordData = new java.util.HashMap<>();
        passwordData.put("password", "newPassword");
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = userController.changePasswordByUsername("nonexistent", passwordData, authentication);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService).findByUsername("nonexistent");
        verify(userService, never()).changePassword(anyLong(), anyString());
    }

    @Test
    void changePasswordByUsername_EmptyPassword_ReturnsBadRequest() {
        // Given
        java.util.Map<String, String> passwordData = new java.util.HashMap<>();
        passwordData.put("password", "");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<?> response = userController.changePasswordByUsername("testuser", passwordData, authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Password cannot be empty", response.getBody());
        verify(userService).findByUsername("testuser");
        verify(userService, never()).changePassword(anyLong(), anyString());
    }

    @Test
    void changePasswordByUsername_NullPassword_ReturnsBadRequest() {
        // Given
        java.util.Map<String, String> passwordData = new java.util.HashMap<>();
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<?> response = userController.changePasswordByUsername("testuser", passwordData, authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Password cannot be empty", response.getBody());
        verify(userService).findByUsername("testuser");
        verify(userService, never()).changePassword(anyLong(), anyString());
    }

    @Test
    void changePasswordByUsername_InvalidData_ReturnsBadRequest() {
        // Given
        java.util.Map<String, String> passwordData = new java.util.HashMap<>();
        passwordData.put("password", "newPassword");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new IllegalArgumentException("Password does not meet requirements"))
                .when(userService).changePassword(1L, "newPassword");

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> userController.changePasswordByUsername("testuser", passwordData, authentication));

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Password does not meet requirements", body.get("error"));
        verify(userService).findByUsername("testuser");
        verify(userService).changePassword(1L, "newPassword");
    }

    @Test
    void changePasswordByUsername_ServiceException_ReturnsInternalServerError() {
        // Given
        java.util.Map<String, String> passwordData = new java.util.HashMap<>();
        passwordData.put("password", "newPassword");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new RuntimeException("Database error"))
                .when(userService).changePassword(1L, "newPassword");

        // When
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> userController.changePasswordByUsername("testuser", passwordData, authentication));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").toString().contains("Internal server error"));
        verify(userService).findByUsername("testuser");
        verify(userService).changePassword(1L, "newPassword");
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
        ResponseEntity<?> response = invokeControllerWithExceptionHandling(() -> userController.getAllUsers());

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").toString().contains("Internal server error"));
        verify(userService).getAllUsers();
    }
}
