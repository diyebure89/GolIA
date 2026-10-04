package com.diyebure.golia.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Emision y validacion de JSON Web Tokens (HS256).
 *
 * <p>Genera dos tipos de token: {@code access} (corto) y {@code refresh}
 * (largo), diferenciados por el claim {@code type}. El subject del token es el
 * id del usuario. La clave y las expiraciones se leen de configuracion.</p>
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    public JwtService(
            @Value("${golia.jwt.secret}") String secret,
            @Value("${golia.jwt.access-expiration-seconds}") long accessExpirationSeconds,
            @Value("${golia.jwt.refresh-expiration-seconds}") long refreshExpirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    public String generateAccessToken(String userId) {
        return build(userId, TYPE_ACCESS, accessExpirationSeconds);
    }

    public String generateRefreshToken(String userId) {
        return build(userId, TYPE_REFRESH, refreshExpirationSeconds);
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    private String build(String userId, String type, long expirationSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000);
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Valida la firma/expiracion y devuelve el id del usuario (subject).
     *
     * @throws io.jsonwebtoken.JwtException si el token es invalido o expiro
     */
    public String extractUserId(String token) {
        return parse(token).getSubject();
    }

    /** @return true si el token es un refresh token valido. */
    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parse(token).get(CLAIM_TYPE, String.class));
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
