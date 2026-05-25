package com.miniuber.core.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.core.auth.dto.AuthResponse;
import com.miniuber.core.auth.dto.LoginRequest;
import com.miniuber.core.auth.dto.RegisterRequest;
import com.miniuber.core.auth.dto.ValidateTokenResponse;
import com.miniuber.core.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.miniuber.core.auth.util.JwtUtil jwtUtil;

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setPhone("1234567890");
        request.setUserType("RIDER");

        AuthResponse response = AuthResponse.builder()
                .accessToken("jwt-token")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        AuthResponse response = AuthResponse.builder()
                .accessToken("jwt-token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void validateToken_Success() throws Exception {
        ValidateTokenResponse response = ValidateTokenResponse.builder()
                .valid(true)
                .email("test@example.com")
                .userType("RIDER")
                .build();

        when(authService.validateToken("valid-token")).thenReturn(response);

        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
