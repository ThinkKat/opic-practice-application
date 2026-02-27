package me.thinkcat.opic.practice.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import me.thinkcat.opic.practice.entity.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenValidity;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, Long userId, UserRole role) {
        return buildToken(username, userId, role, accessTokenValidity);
    }

    public String generateRefreshToken(String username, Long userId) {
        return buildToken(username, userId, null, refreshTokenValidity);
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidity / 1000;
    }

    public long getRefreshTokenValidityInMillis() {
        return refreshTokenValidity;
    }

    private String buildToken(String username, Long userId, UserRole role, long validityInMilliseconds) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(validity);

        if (role != null) {
            builder.claim("role", role.getCode());
        }

        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    public UserRole getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String role = claims.get("role", String.class);
        return role != null ? UserRole.fromCode(role) : UserRole.FREE;
    }

    /**
     * @return TokenValidationResult.VALID, EXPIRED, or INVALID
     */
    public TokenValidationResult validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenValidationResult.INVALID;
        }
    }

    public enum TokenValidationResult {
        VALID, EXPIRED, INVALID
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Long.class);
    }
}
