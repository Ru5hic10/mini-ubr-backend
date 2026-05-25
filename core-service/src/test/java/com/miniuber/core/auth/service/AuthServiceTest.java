package com.miniuber.core.auth.service;

import com.miniuber.core.auth.config.JwtConfig;
import com.miniuber.core.auth.dto.AuthResponse;
import com.miniuber.core.auth.dto.LoginRequest;
import com.miniuber.core.auth.dto.RegisterRequest;
import com.miniuber.core.auth.event.UserRegistrationEventPublisher;
import com.miniuber.core.auth.repository.RefreshTokenRepository;
import com.miniuber.core.auth.util.JwtUtil;
import com.miniuber.core.driver.dto.DriverResponse;
import com.miniuber.core.driver.service.DriverService;
import com.miniuber.core.user.dto.AuthUserDTO;
import com.miniuber.core.user.dto.UserResponse;
import com.miniuber.core.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRegistrationEventPublisher eventPublisher;

    @Mock
    private UserService userService;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Common setup if needed
        lenient().when(jwtConfig.getExpiration()).thenReturn(3600000L);
        lenient().when(jwtConfig.getRefreshExpiration()).thenReturn(86400000L);
    }

    @Test
    void register_Rider_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Rider John");
        request.setEmail("rider@example.com");
        request.setPassword("password");
        request.setUserType("RIDER");

        UserResponse mockUserResponse = new UserResponse(1L, "Rider John", "rider@example.com", "123", null, 5.0,
                "RIDER", true, null);
        when(userService.registerUser(any())).thenReturn(mockUserResponse);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("refresh-token");

        com.miniuber.core.user.entity.User mockUserEntity = new com.miniuber.core.user.entity.User();
        mockUserEntity.setName("Rider John");
        mockUserEntity.setPhone("123");
        when(userService.getUserByEmail(anyString())).thenReturn(mockUserEntity);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("bearer", response.getTokenType().toLowerCase());
        assertEquals("access-token", response.getAccessToken());
        verify(userService, times(1)).registerUser(any());
        verify(eventPublisher, times(1)).publishRiderRegistration(any(), any(), any());
    }

    @Test
    void register_Driver_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Driver Bob");
        request.setEmail("driver@example.com");
        request.setPassword("password");
        request.setUserType("DRIVER");

        DriverResponse mockDriverResponse = new DriverResponse();
        mockDriverResponse.setId(2L);
        mockDriverResponse.setName("Driver Bob");
        mockDriverResponse.setEmail("driver@example.com");

        when(driverService.registerDriver(any())).thenReturn(mockDriverResponse);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("refresh-token");

        com.miniuber.core.driver.entity.Driver mockDriverEntity = new com.miniuber.core.driver.entity.Driver();
        mockDriverEntity.setName("Driver Bob");
        mockDriverEntity.setPhone("123");
        when(driverService.getDriverByEmail(anyString())).thenReturn(mockDriverEntity);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(driverService, times(1)).registerDriver(any());
        verify(eventPublisher, times(1)).publishDriverRegistration(any(), any(), any());
    }

    @Test
    void login_Rider_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("rider@example.com");
        request.setPassword("password");
        request.setUserType("RIDER");

        AuthUserDTO mockAuthUser = new AuthUserDTO(1L, "Rider John", "rider@example.com", "encodedPassword", "123");
        when(userService.getAuthUserByEmail("rider@example.com")).thenReturn(mockAuthUser);
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("access-token");
        when(userService.getUserByEmail("rider@example.com")).thenReturn(new com.miniuber.core.user.entity.User()); // For
                                                                                                                    // details
                                                                                                                    // fetching

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
    }
}
