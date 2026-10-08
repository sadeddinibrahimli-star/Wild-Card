package com.wildcard.auth;

import com.wildcard.config.WildcardProperties;
import com.wildcard.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@Slf4j
public class JwtService {
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final String issuer;

    public JwtService(WildcardProperties props) {
        String secret = resolveSecret(props.getJwt().getSecret(), props.isRequireSecrets());
        long accessMinutes = props.getJwt().getAccessTokenExpirationMinutes();
        long refreshDays = props.getJwt().getRefreshTokenExpirationDays();
        String issuer = props.getJwt().getIssuer();

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = Duration.ofMinutes(accessMinutes);
        this.refreshTokenTtl = Duration.ofDays(refreshDays);
        this.issuer = issuer;
    }

    /**
     * No crash when the key is missing: development gets a generated
     * temporary key and a warning is logged. In production the environment
     * variable must be provided.
     */
    private static String resolveSecret(String configured, boolean required) {
        if (configured != null && !configured.isBlank()) {
            if (configured.length() < 32) {
                log.warn("JWT_SECRET is shorter than 32 characters - a strong secret is required");
            }
            return configured;
        }

        if (required) {
            throw new IllegalStateException(
                    "JWT_SECRET is required in this environment but it is empty. "
                            + "Set JWT_SECRET (32+ chars) before starting the application.");
        }

        log.warn("JWT_SECRET is not set - using an ephemeral DEVELOPMENT-ONLY secret. "
                + "This must never happen in production.");
        return "wild-card-development-only-" + java.util.UUID.randomUUID()
                + "-" + java.util.UUID.randomUUID();
    }

    public String generateAccessToken(User user) {
        return build(user, TYPE_ACCESS, accessTokenTtl);
    }

    public String generateRefreshToken(User user) {
        return build(user, TYPE_REFRESH, refreshTokenTtl);
    }

    private String build(User user, String type, Duration ttl) {
        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String extractRole(String token) {
        return parse(token).get(CLAIM_ROLE, String.class);
    }

    public boolean isOfType(String token, String type) {
        return type.equals(parse(token).get(CLAIM_TYPE, String.class));
    }

    public long getAccessTokenSeconds() {
        return accessTokenTtl.toSeconds();
    }

    public long getRefreshTokenSeconds() {
        return refreshTokenTtl.toSeconds();
    }
}
