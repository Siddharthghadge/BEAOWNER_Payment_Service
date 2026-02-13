package com.carrental.paymentservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ✅ FIXED: Always return ROLE_ format + UPPERCASE
    public String extractRole(String token) {
        String role = extractAllClaims(token).get("role", String.class);

        if (role == null || role.isBlank()) {
            return "ROLE_CUSTOMER";
        }

        role = role.trim();

        // if token gives "Customer" -> "CUSTOMER"
        role = role.toUpperCase();

        // if token gives "ROLE_CUSTOMER" keep it
        if (role.startsWith("ROLE_")) {
            return role;
        }

        return "ROLE_" + role;
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        Date exp = extractExpiration(token);
        return exp != null && exp.before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
