package com.hotel.booking.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/** Issues and verifies the stateless JWTs used by the Angular single-page app. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${hotel.jwt-secret}") String secret,
                      @Value("${hotel.jwt-hours:24}") int hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = hours * 3600_000L;
    }

    public String issue(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("u", username))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    /** Returns the user id, or null when the token is missing, expired or forged. */
    public Long parseUserId(String token) {
        try {
            var payload = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return Long.valueOf(payload.getSubject());
        } catch (Exception ex) {
            return null;
        }
    }
}
