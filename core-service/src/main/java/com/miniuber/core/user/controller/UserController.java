package com.miniuber.core.user.controller;

import com.miniuber.core.user.dto.LoginRequest;
import com.miniuber.core.user.dto.UserRegistrationRequest;
import com.miniuber.core.user.dto.UserResponse;
import com.miniuber.core.user.dto.UserUpdateRequest;
import com.miniuber.core.user.entity.User;
import com.miniuber.core.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request) {
        UserResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is running!");
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    /**
     * Internal endpoint for auth service - returns user with password
     */
    @GetMapping("/internal/auth/{email}")
    public ResponseEntity<com.miniuber.core.user.dto.AuthUserDTO> getAuthUserByEmail(@PathVariable String email) {
        com.miniuber.core.user.dto.AuthUserDTO user = userService.getAuthUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}
