package com.miniuber.core.auth.repository;

import com.miniuber.core.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(Long userId);
    Optional<RefreshToken> findByUserIdAndUserType(Long userId, String userType);
}