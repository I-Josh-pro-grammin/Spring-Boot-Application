package org.josh.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.josh.store.Dtos.LoginAndRegisterResponseDto;
import org.josh.store.Dtos.LoginUserDto;
import org.josh.store.Dtos.RegisterUserDto;
import org.josh.store.Dtos.UserDto;
import org.josh.store.config.JwtService;
import org.josh.store.model.User;
import org.josh.store.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(authController.class)
@org.springframework.context.annotation.Import(org.josh.store.config.SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterUserDto registerDto = new RegisterUserDto("password123", "johndoe", "john@example.com", new Date());
        User createdUser = new User("john@example.com", "johndoe", "encodedPassword", new Date());
        
        when(userService.registerUser(any(RegisterUserDto.class))).thenReturn(createdUser);
        when(jwtService.generateToken("john@example.com")).thenReturn("mock-jwt-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void testRegister_UserAlreadyExists() throws Exception {
        // Arrange
        RegisterUserDto registerDto = new RegisterUserDto("password123", "johndoe", "john@example.com", new Date());
        when(userService.registerUser(any(RegisterUserDto.class))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User already exists"));
    }

    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        LoginUserDto loginDto = new LoginUserDto("password123", "john@example.com");
        UserDto userDto = new UserDto(UUID.randomUUID(), "johndoe", "john@example.com", new Date());

        when(userService.login(any(LoginUserDto.class))).thenReturn(userDto);
        when(jwtService.generateToken("john@example.com")).thenReturn("mock-jwt-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        // Arrange
        LoginUserDto loginDto = new LoginUserDto("password123", "nonexistent@example.com");
        when(userService.login(any(LoginUserDto.class))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User not found"));
    }
}
