package com.wms.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Tiện ích JWT (Spec 001).
 * Tạo access token (chứa email + role), parse token, trích xuất email, kiểm tra hợp lệ.
 * Secret key và TTL đọc từ application properties.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpiry;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
    }

    /** Tạo JWT access token: subject = email, claim role, hạn = accessTokenExpiry giây. */
    public String generateAccessToken(String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpiry * 1000))
                .signWith(key)
                .compact();
    }

    /** Parse và xác thực chữ ký JWT — throw nếu token không hợp lệ hoặc hết hạn. */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Trích xuất email (subject) từ JWT token. */
    public String extractEmail(String token) {
        return parseToken(token).getSubject();
    }

    /** Kiểm tra token có hợp lệ (chữ ký đúng, chưa hết hạn). */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
