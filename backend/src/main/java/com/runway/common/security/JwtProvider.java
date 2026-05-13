package com.runway.common.security;

import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    private void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TYPE_ACCESS      = "access";
    private static final String TYPE_REFRESH     = "refresh";

    public String generateAccessToken(UUID userId, String email) {
        return buildToken(userId, email, jwtProperties.getAccessTokenExpiry(), TYPE_ACCESS);
    }

    public String generateRefreshToken(UUID userId, String email) {
        return buildToken(userId, email, jwtProperties.getRefreshTokenExpiry(), TYPE_REFRESH);
    }

    /**
     * Bearer 필터 전용. 서명·만료 검증 + tokenType == "access" 까지 확인한다.
     * refresh token을 Bearer 인증에 사용하는 것을 차단한다.
     */
    public boolean isValidAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    // reissue 전용: 만료 vs 위조를 구분하고 tokenType == "refresh" 를 추가로 검증한다
    public UUID validateRefreshTokenAndGetUserId(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
                throw new RunwayException(ErrorCode.INVALID_TOKEN);
            }
            return UUID.fromString(claims.getSubject());
        } catch (RunwayException e) {
            throw e;
        } catch (ExpiredJwtException e) {
            throw new RunwayException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new RunwayException(ErrorCode.INVALID_TOKEN);
        }
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String buildToken(UUID userId, String email, long expiryMs, String tokenType) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expiryMs);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
