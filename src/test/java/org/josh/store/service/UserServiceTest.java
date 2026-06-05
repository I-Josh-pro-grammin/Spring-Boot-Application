package org.josh.store.service;

import org.josh.store.Dtos.LoginUserDto;
import org.josh.store.Dtos.RegisterUserDto;
import org.josh.store.Dtos.UserDto;
import org.josh.store.model.User;
import org.josh.store.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void testRegisterUser_Success() {
        // Arrange
        RegisterUserDto dto = new RegisterUserDto("password123", "johndoe", "john@example.com", new Date());
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(null);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedPassword");

        User savedUser = new User("john@example.com", "johndoe", "encodedPassword", new Date());
        savedUser.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.registerUser(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        verify(userRepository, times(1)).findByEmail("john@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_UserAlreadyExists() {
        // Arrange
        RegisterUserDto dto = new RegisterUserDto("password123", "johndoe", "john@example.com", new Date());
        User existingUser = new User();
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(existingUser);

        // Act
        User result = userService.registerUser(dto);

        // Assert
        assertThat(result).isNull();
        verify(userRepository, times(1)).findByEmail("john@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUserByEmail() {
        // Arrange
        UUID id = UUID.randomUUID();
        Date createdAt = new Date();
        User user = new User(id, "johndoe", "password", "john@example.com", createdAt);
        when(userRepository.findByEmail("john@example.com")).thenReturn(user);

        // Act
        UserDto result = userService.getUserByEmail("john@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void testLogin_Success() {
        // Arrange
        LoginUserDto loginDto = new LoginUserDto("password123", "john@example.com");
        User user = new User(UUID.randomUUID(), "johndoe", "encodedPassword", "john@example.com", new Date());
        when(userRepository.findByEmail("john@example.com")).thenReturn(user);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // Act
        UserDto result = userService.login(loginDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getUsername()).isEqualTo("johndoe");
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        LoginUserDto loginDto = new LoginUserDto("password123", "nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        // Act
        UserDto result = userService.login(loginDto);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void testLogin_WrongPassword() {
        // Arrange
        LoginUserDto loginDto = new LoginUserDto("wrong_password", "john@example.com");
        User user = new User(UUID.randomUUID(), "johndoe", "encodedPassword", "john@example.com", new Date());
        when(userRepository.findByEmail("john@example.com")).thenReturn(user);
        when(passwordEncoder.matches("wrong_password", "encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.login(loginDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password is incorrect");
    }
}
