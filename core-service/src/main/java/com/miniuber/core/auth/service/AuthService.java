package com.miniuber.core.auth.service;

import com.miniuber.core.auth.config.JwtConfig;
import com.miniuber.core.auth.dto.*;
import com.miniuber.core.auth.entity.RefreshToken;
import com.miniuber.core.auth.event.UserRegistrationEventPublisher;
import com.miniuber.core.auth.exception.InvalidCredentialsException;
import com.miniuber.core.auth.repository.RefreshTokenRepository;
import com.miniuber.core.auth.util.JwtUtil;
import com.miniuber.core.driver.dto.DriverRegistrationRequest;
import com.miniuber.core.driver.dto.DriverResponse;
import com.miniuber.core.driver.entity.Driver;
import com.miniuber.core.driver.service.DriverService;
import com.miniuber.core.user.dto.UserRegistrationRequest;
import com.miniuber.core.user.dto.UserResponse;
import com.miniuber.core.user.entity.User;
import com.miniuber.core.user.service.UserService;
import com.miniuber.core.auth.controller.LoggingController;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRegistrationEventPublisher eventPublisher;

    // Direct Service Injection
    private final UserService userService;
    private final DriverService driverService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("User registration attempt - Email: {}, UserType: {}", request.getEmail(), request.getUserType());
        // LoggingController.addLog(
        // "User registration attempt - Email: " + request.getEmail() + ", UserType: " +
        // request.getUserType());

        // Hash password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        Long userId;
        String email;
        String name;

        if ("DRIVER".equalsIgnoreCase(request.getUserType())) {
            DriverRegistrationRequest driverRequest = new DriverRegistrationRequest();
            driverRequest.setName(request.getName());
            driverRequest.setEmail(request.getEmail());
            driverRequest.setPassword(request.getPassword()); // DriverService hashes it too? Check. Yes it does in
                                                              // registerDriver.
            // Wait, AuthService hashed it above? DriverService.registerDriver hashes it
            // AGAIN.
            // I should pass raw password if DriverService hashes it, OR modify
            // DriverService.
            // Looking at legacy code: AuthService sent hashed password?
            // Line 54: serviceRequest.put("password", hashedPassword);
            // So UserService/DriverService received HASHED password.
            // Let's check UserService code again.
            // UserService: user.setPassword(passwordEncoder.encode(request.getPassword()));
            // If I pass hashed password, it will be DOUBLE HASHED.
            // BUT, the WebClient call sent the "hashedPassword" map.
            // So legacy UserService was receiving hashed password and hashing it again?
            // Or maybe UserService didn't hash if it was from "internal"?
            // Step 115: UserService.registerUser takes UserRegistrationRequest and calls
            // passwordEncoder.encode
            // So if I pass raw password here, UserService will hash it.
            // If I pass hashed password, UserService will hash the hash.
            // The original AuthService HASHED it before sending.
            // Example:
            // 1. request.pass = "123"
            // 2. hashed = "bcrypt(123)"
            // 3. send "bcrypt(123)" to UserService
            // 4. UserService.registerUser sees request.password = "bcrypt(123)"
            // 5. UserService stores "bcrypt(bcrypt(123))"
            // Start. Login:
            // AuthService.login validates against what?
            // AuthService fetches user, gets stored password.
            // matches(input, stored).
            // If double hashed, this works ONLY if I double hash check?
            // Actually, let's look at AuthService.login again.
            // Line 145: passwordEncoder.matches(request.getPassword(), storedPassword);
            // This is standard single hash check.
            // Conclusion: ORIGINAL CODE WAS DOUBLE HASHING? Or UserService endpoint was
            // DIFFERENT?
            // Original call: userServiceUrl + "/api/users/register"
            // UserService.registerUser is mapped to that.
            // It clearly hashes given password.
            // So stored password IS double hashed? This is weird.
            // UNLESS the map keys were different?
            // AuthService sending map: "password" -> hashed.
            // UserService request DTO: fields matching JSON.
            // So yes, double hashing.
            // I will correct this "feature" or replicate it.
            // Logic: I'll pass RAW password to UserService/DriverService and let THEM hash
            // it.
            // And I will NOT hash it here in AuthService (except maybe for checking?).
            // But AuthService needs to know if password matches on Login.
            // For Login, I fetch user, get stored hash, and check.
            // So for Register, I should just lelegate to Service.

            driverRequest.setPassword(request.getPassword()); // Pass RAW
            driverRequest.setPhone(request.getPhone());
            driverRequest.setLicenseNumber(request.getLicenseNumber());
            driverRequest.setVehicleType(request.getVehicleType());
            driverRequest.setVehicleNumber(request.getVehicleNumber());
            driverRequest.setVehicleModel(request.getVehicleModel());

            DriverResponse response = driverService.registerDriver(driverRequest);
            userId = response.getId();
            email = response.getEmail();
            name = response.getName();

        } else {
            UserRegistrationRequest userRequest = new UserRegistrationRequest();
            userRequest.setName(request.getName());
            userRequest.setEmail(request.getEmail());
            userRequest.setPassword(request.getPassword()); // Pass RAW
            userRequest.setPhone(request.getPhone());

            UserResponse response = userService.registerUser(userRequest);
            userId = response.getId();
            email = response.getEmail();
            name = response.getName();
        }

        logger.info("User registered successfully - Email: {}, UserId: {}", email, userId);
        // LoggingController.addLog("User registered successfully - Email: " + email +
        // ", UserId: " + userId);

        // Publish registration event to Kafka
        if ("DRIVER".equalsIgnoreCase(request.getUserType())) {
            eventPublisher.publishDriverRegistration(userId, email, name);
        } else {
            eventPublisher.publishRiderRegistration(userId, email, name);
        }

        // Generate tokens
        return generateAuthResponse(userId, email, request.getUserType());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        logger.info("User login attempt - Email: {}, UserType: {}", request.getEmail(), request.getUserType());
        // LoggingController.addLog("User login attempt - Email: " +
        // request.getEmail());

        // Default to RIDER if userType is not provided
        String userType = request.getUserType() != null ? request.getUserType().toUpperCase() : "RIDER";

        Long userId;
        String email;
        String name;
        String phone;
        String storedPassword;

        if ("DRIVER".equals(userType)) {
            try {
                Driver driver = driverService.getDriverByEmail(request.getEmail());
                userId = driver.getId();
                email = driver.getEmail();
                name = driver.getName();
                phone = driver.getPhone();
                storedPassword = driver.getPassword();
            } catch (Exception e) {
                logger.error("Driver not found - Email: {}", request.getEmail());
                throw new InvalidCredentialsException("Invalid email or password");
            }
        } else {
            try {
                // Using getAuthUserByEmail to get password (DTO might need updating if not
                // exposing password)
                // UserService.getAuthUserByEmail returns AuthUserDTO which HAS password.
                com.miniuber.core.user.dto.AuthUserDTO user = userService.getAuthUserByEmail(request.getEmail());
                userId = user.getId();
                email = user.getEmail();
                name = user.getName();
                phone = user.getPhone();
                storedPassword = user.getPassword();
            } catch (Exception e) {
                logger.error("User not found - Email: {}", request.getEmail());
                throw new InvalidCredentialsException("Invalid email or password");
            }
        }

        // Verify password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), storedPassword);

        if (!passwordMatches) {
            logger.error("Invalid password for user - Email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        logger.info("User login successful - Email: {}, UserType: {}, UserId: {}", email, userType, userId);
        // LoggingController.addLog("User login successful - Email: " + email + ",
        // UserType: " + userType);

        return generateAuthResponseWithDetails(userId, email, name, phone, userType);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token has expired");
        }

        // Validate the refresh token JWT
        if (!jwtUtil.validateToken(refreshToken.getToken())) {
            throw new RuntimeException("Invalid refresh token");
        }

        String email = jwtUtil.extractEmail(refreshToken.getToken());

        return generateAuthResponse(
                refreshToken.getUserId(),
                email,
                refreshToken.getUserType());
    }

    public ValidateTokenResponse validateToken(String token) {
        try {
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.extractUserId(token);
                String email = jwtUtil.extractEmail(token);
                String userType = jwtUtil.extractUserType(token);

                return ValidateTokenResponse.builder()
                        .valid(true)
                        .userId(userId)
                        .email(email)
                        .userType(userType)
                        .message("Token is valid")
                        .build();
            }
        } catch (Exception e) {
            // Token is invalid
        }

        return ValidateTokenResponse.builder()
                .valid(false)
                .message("Token is invalid or expired")
                .build();
    }

    @Transactional
    public void logout(String token) {
        Long userId = jwtUtil.extractUserId(token);
        String userType = jwtUtil.extractUserType(token);

        refreshTokenRepository.findByUserIdAndUserType(userId, userType)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    private AuthResponse generateAuthResponse(Long userId, String email, String userType) {
        // Fetch details to populate full response
        String name = "";
        String phone = "";
        try {
            if ("DRIVER".equalsIgnoreCase(userType)) {
                Driver driver = driverService.getDriverByEmail(email);
                name = driver.getName();
                phone = driver.getPhone();
            } else {
                User user = userService.getUserByEmail(email);
                name = user.getName();
                phone = user.getPhone();
            }
        } catch (Exception e) {
            logger.warn("Could not fetch user details for response", e);
        }
        return generateAuthResponseWithDetails(userId, email, name, phone, userType);
    }

    private AuthResponse generateAuthResponseWithDetails(Long userId, String email, String name, String phone,
            String userType) {
        // Generate access token
        String accessToken = jwtUtil.generateToken(userId, email, userType);

        // Generate refresh token
        String refreshTokenStr = jwtUtil.generateRefreshToken(userId, email, userType);

        // Save refresh token to database
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setUserId(userId);
        refreshToken.setUserType(userType);
        refreshToken.setExpiryDate(
                LocalDateTime.now().plusSeconds(jwtConfig.getRefreshExpiration() / 1000));
        refreshToken.setRevoked(false);

        // Delete old refresh tokens for this user
        refreshTokenRepository.deleteByUserId(userId);
        refreshTokenRepository.save(refreshToken);

        UserInfo userInfo = new UserInfo(userId, name, email, phone, userType);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .user(userInfo)
                .build();
    }
}