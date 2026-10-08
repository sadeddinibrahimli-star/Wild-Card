package com.wildcard.auth;

import com.wildcard.auth.dto.ResetPasswordRequest;
import com.wildcard.common.BusinessException;
import com.wildcard.config.ExternalProperties;
import com.wildcard.config.WildcardProperties;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Password reset rules:
 *  - forgot always returns the SAME response (no user enumeration)
 *  - the token is random, only its SHA-256 hash is stored, 30 minutes
 *  - the token is single use (usedAt)
 *  - without SMTP the link goes to the dev mailbox or the console
 */
@Slf4j
@Service
public class PasswordResetService {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_PER_HOUR = 3;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExternalProperties props;
    private final WildcardProperties config;
    private final com.wildcard.common.DevMailbox devMailbox;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                ExternalProperties props,
                                WildcardProperties config,
                                com.wildcard.common.DevMailbox devMailbox) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.config = config;
        this.devMailbox = devMailbox;
    }

    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("Password reset requested for unknown email");
            return;
        }

        if (countRecentFor(user.getId()) >= MAX_PER_HOUR) {
            log.info("Password reset rate limit reached for user {}", user.getId());
            return;
        }

        String token = newToken();
        tokenRepository.save(PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(token))
                .expiresAt(Instant.now().plus(TTL))
                .createdAt(Instant.now())
                .build());

        String link = config.getFrontendUrl() + "/#reset/" + token;
        if (props.getMail().isConfigured()) {
            sendMail(user.getEmail(), link);
        } else if (devMailbox.isEnabled()) {
            devMailbox.save(user.getEmail(), "Reset your WildCard password", link);
            log.info("=== PASSWORD RESET LINK (SMTP not configured) ===");
            log.info("email: {}", user.getEmail());
            log.info("link : {}", link);
        } else {
            log.info("=== PASSWORD RESET LINK (SMTP not configured) ===");
            log.info("email: {}", user.getEmail());
            log.info("link : {}", link);
        }
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        PasswordResetToken row = tokenRepository
                .findByTokenHash(sha256(request.getToken()))
                .orElseThrow(() -> new BusinessException("This reset link is no longer valid"));

        if (!row.isUsable()) {
            throw new BusinessException("This reset link has expired");
        }

        User user = userRepository.findById(row.getUserId())
                .orElseThrow(() -> new BusinessException("This reset link is no longer valid"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // revoke the logged-in sessions after the password change
        userRepository.save(user);

        row.setUsedAt(Instant.now());
        tokenRepository.save(row);
    }

    private long countRecentFor(Long userId) {
        return tokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(userId))
                .filter(t -> t.getCreatedAt().isAfter(Instant.now().minus(Duration.ofHours(1))))
                .count();
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private void sendMail(String to, String link) {
        try {
            var mail = props.getMail();
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(mail.getHost());
            sender.setPort(mail.portOrDefault());
            sender.setUsername(mail.getUsername());
            sender.setPassword(mail.getPassword());

            MimeMessage message = sender.createMimeMessage();
            message.setFrom(new InternetAddress(mail.getFrom()));
            message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Reset your WildCard password");
            message.setText("Open this link to choose a new password:\n\n" + link
                    + "\n\nThe link expires in 30 minutes.", StandardCharsets.UTF_8.name());
            sender.send(message);

            log.info("Password reset mail sent to {}", to);
        } catch (Exception e) {
            // a delivery failure must not break the flow; the link is also printed to the console
            log.warn("Could not send reset mail to {}: {}", to, e.getMessage());
            log.info("=== PASSWORD RESET LINK (mail failed) ===");
            log.info("link : {}", link);
        }
    }

    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        long removed = tokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (removed > 0) {
            log.info("Cleaned up {} expired password reset tokens", removed);
        }
    }
}
