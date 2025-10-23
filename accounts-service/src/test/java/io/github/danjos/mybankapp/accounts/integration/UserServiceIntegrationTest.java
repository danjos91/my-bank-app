package io.github.danjos.mybankapp.accounts.integration;

import io.github.danjos.mybankapp.accounts.AccountsServiceApplication;
import io.github.danjos.mybankapp.accounts.config.TestJpaConfig;
import io.github.danjos.mybankapp.accounts.dto.UserProfileDTO;
import io.github.danjos.mybankapp.accounts.dto.UserRegistrationDTO;
import io.github.danjos.mybankapp.accounts.entity.User;
import io.github.danjos.mybankapp.accounts.repository.UserRepository;
import io.github.danjos.mybankapp.accounts.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {AccountsServiceApplication.class, TestJpaConfig.class})
@Testcontainers
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        UserRegistrationDTO registrationDTO = UserRegistrationDTO.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        // When
        User registeredUser = userService.registerUser(registrationDTO);

        // Then
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getId()).isNotNull();
        assertThat(registeredUser.getUsername()).isEqualTo("newuser");
        assertThat(registeredUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(registeredUser.getFirstName()).isEqualTo("New");
        assertThat(registeredUser.getLastName()).isEqualTo("User");
        assertThat(registeredUser.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(registeredUser.getCreatedAt()).isNotNull();
        assertThat(registeredUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindUserById() {
        // Given
        User user = createTestUser();
        user = userRepository.save(user);

        // When
        Optional<User> foundUser = userService.findById(user.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(user.getId());
        assertThat(foundUser.get().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    void shouldFindUserByUsername() {
        // Given
        User user = createTestUser();
        user = userRepository.save(user);

        // When
        Optional<User> foundUser = userService.findByUsername(user.getUsername());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(user.getId());
        assertThat(foundUser.get().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    void shouldFindUserByEmail() {
        // Given
        User user = createTestUser();
        user = userRepository.save(user);

        // When
        Optional<User> foundUser = userRepository.findByEmail(user.getEmail());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(user.getId());
        assertThat(foundUser.get().getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldUpdateUserProfile() {
        // Given
        User user = createTestUser();
        user = userRepository.save(user);

        UserProfileDTO profileDTO = UserProfileDTO.builder()
                .firstName("Updated")
                .lastName("Name")
                .email("updated@example.com")
                .build();

        // When
        User updatedUser = userService.updateUserProfile(user.getId(), profileDTO);

        // Then
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
        assertThat(updatedUser.getLastName()).isEqualTo("Name");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getUpdatedAt()).isAfter(user.getUpdatedAt());
    }

    @Test
    void shouldChangePassword() {
        // Given
        User user = createTestUser();
        user = userRepository.save(user);
        String newPassword = "newpassword123";

        // When
        userService.changePassword(user.getId(), newPassword);

        // Then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPassword()).isNotEqualTo(user.getPassword());
    }

    @Test
    void shouldDeleteUser() {
        // Given
        User user = createTestUser();
        user = userRepository.save(user);

        // When
        userService.deleteUser(user.getId());

        // Then
        Optional<User> deletedUser = userService.findById(user.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void shouldGetAllUsers() {
        // Given
        User user1 = createTestUser();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        userRepository.save(user1);

        User user2 = createTestUser();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        userRepository.save(user2);

        // When
        List<UserProfileDTO> users = userService.getAllUsers();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserProfileDTO::getUsername).contains("user1", "user2");
    }

    @Test
    void shouldHandleNonExistentUser() {
        // When
        Optional<User> user = userService.findById(999L);

        // Then
        assertThat(user).isEmpty();
    }

    private User createTestUser() {
        return User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
    }
}
