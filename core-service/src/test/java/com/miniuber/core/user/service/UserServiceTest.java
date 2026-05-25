package com.miniuber.core.user.service;

import com.miniuber.core.user.dto.LoginRequest;
import com.miniuber.core.user.dto.UserRegistrationRequest;
import com.miniuber.core.user.dto.UserResponse;
import com.miniuber.core.user.entity.User;
import com.miniuber.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    // Use spy or real object for utility classes if possible, or mock them if
    // strictly injected
    // UserService instantiates BCryptPasswordEncoder internally with 'new',
    // so we cannot mock it easily without PowerMock or Refactoring.
    // However, since it's a real simplified object, we can just let it run.

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword(new BCryptPasswordEncoder().encode("password")); // Real encoding matches internal logic
        testUser.setRole("RIDER");
        testUser.setActive(true);
    }

    @Test
    void registerUser_Success() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setPhone("1234567890");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test User", response.getName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_EmailExists() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        UserResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void login_InvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong_password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> userService.login(request));
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }
}
