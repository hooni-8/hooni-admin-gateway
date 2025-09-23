package org.admin.gateway.filters.jwt;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import io.jsonwebtoken.security.Keys;
import org.admin.gateway.common.model.LoginStatus;
import org.admin.gateway.entity.user.UserDetail;
import org.admin.gateway.properties.JwtProperties;

import javax.crypto.SecretKey;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    private static final long ACCESS_TOKEN_VALIDITY = 30 * 60 * 1000L;     // 30분
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L; // 7일 (ms)

    private static final String ACCESS_SUBJECT = "accessToken";
    private static final String REFRESH_SUBJECT = "refreshToken";

    public JwtTokenProvider(JwtProperties jwtProperties) {
        String base64SecretKey = jwtProperties.getSecretKey();
        this.secretKey = Keys.hmacShaKeyFor(base64SecretKey.getBytes());
    }

    public String generateAccessToken(UserDetail user) {
        return createToken(user, ACCESS_TOKEN_VALIDITY, ACCESS_SUBJECT);
    }

    public String generateRefreshToken(UserDetail user) {
        return createToken(user, REFRESH_TOKEN_EXPIRE_TIME, REFRESH_SUBJECT);
    }

    private String createToken(UserDetail user, long duration, String subject) {
        Map<String, Object> claims = Map.of(
                "userCode", user.getUserCode(),
                "userName", user.getUserName(),
                "role", user.getRoleGroup()
        );

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + duration))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String getAccessSubject(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getClaimsUserCode(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("userCode", String.class);
    }

    public LoginStatus getClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return LoginStatus.builder()
                .status(true)
                .name(claims.get("userName", String.class))
                .userCode(claims.get("userCode", String.class))
                .role(claims.get("role", String.class))
                .build();
    }
}
