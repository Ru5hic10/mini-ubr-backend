package com.miniuber.core.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.core.user.dto.LoginRequest;
import com.miniuber.core.user.dto.UserRegistrationRequest;
import com.miniuber.core.user.dto.UserResponse;
import com.miniuber.core.user.service.UserService;
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

@WebMvcTest(UserController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private UserService userService;

        @MockBean
        private com.miniuber.core.auth.util.JwtUtil jwtUtil;

        @Test
        void registerUser_Success() throws Exception {
                UserRegistrationRequest request = new UserRegistrationRequest();
                request.setName("Test User");
                request.setEmail("user@example.com");
                request.setPassword("password");
                request.setPhone("1234567890");

                // UserResponse(Long id, String name, String email, String phone, String
                // profilePicture, Double rating, String role, Boolean active, LocalDateTime
                // createdAt)
                UserResponse response = new UserResponse(1L, "Test User", "user@example.com", "1234567890", null, 0.0,
                                "RIDER",
                                true, null);

                when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.email").value("user@example.com"));
        }

        @Test
        void login_Success() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setEmail("user@example.com");
                request.setPassword("password");

                UserResponse response = new UserResponse(1L, "Test User", "user@example.com", "1234567890", null, 0.0,
                                "RIDER",
                                true, null);

                when(userService.login(any(LoginRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        void getUser_Success() throws Exception {
                UserResponse response = new UserResponse(1L, "Test User", "user@example.com", "1234567890", null, 0.0,
                                "RIDER",
                                true, null);

                when(userService.getUserById(1L)).thenReturn(response);

                mockMvc.perform(get("/api/users/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Test User"));
        }

        @Test
        void getUserByEmail_Success() throws Exception {
                com.miniuber.core.user.entity.User user = new com.miniuber.core.user.entity.User();
                user.setId(1L);
                user.setName("Test User");
                user.setEmail("user@example.com");

                when(userService.getUserByEmail("user@example.com")).thenReturn(user);

                mockMvc.perform(get("/api/users/email/user@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("user@example.com"));
        }
}
