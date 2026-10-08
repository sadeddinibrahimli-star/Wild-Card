package com.wildcard.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LOCAL mailbox for development.
 *
 * Without SMTP the password reset link never arrives: the page says
 * "link sent" but the user cannot see it. This fixes it:
 * the backend writes every message to disk and
 * `GET /api/v1/dev/mailbox` returns them.
 *
 * Only active in the dev/docker profile (enabled=... in yml). In production
 * enabled=false and nothing is written.
 */
@Slf4j
@Configuration
public class DevMailbox {

    private static final int MAX_MAILS = 20;

    private final boolean enabled;
    private final Path file;

    public DevMailbox(@Value("${wildcard.dev-mailbox.enabled:false}") boolean enabled,
                      @Value("${wildcard.dev-mailbox.file:./dev-mailbox.log}") String file) {
        this.enabled = enabled;
        this.file = Path.of(file);
        if (enabled) {
            log.info("Dev mailbox ACTIVE ({}). Sent mails are readable at /api/v1/dev/mailbox", file);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void save(String to, String subject, String body) {
        if (!enabled) {
            return;
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }

            Map<String, String> mail = new LinkedHashMap<>();
            mail.put("to", to);
            mail.put("subject", subject);
            mail.put("body", body);
            mail.put("at", Instant.now().toString());

            List<String> existing = Files.exists(file)
                    ? new ArrayList<>(Files.readAllLines(file))
                    : new ArrayList<>();
            existing.removeIf(String::isBlank);
            existing.add(toJson(mail));

            while (existing.size() > MAX_MAILS) {
                existing.remove(0);
            }
            Files.write(file, existing);

            log.info("Dev mailbox: mail saved for {}", to);
        } catch (IOException e) {
            log.warn("Dev mailbox write failed: {}", e.getMessage());
        }
    }

    public List<Map<String, String>> all() {
        List<Map<String, String>> out = new ArrayList<>();
        if (!enabled || !Files.exists(file)) {
            return out;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (!line.isBlank()) {
                    out.add(fromJson(line));
                }
            }
        } catch (IOException e) {
            log.warn("Dev mailbox read failed: {}", e.getMessage());
        }
        java.util.Collections.reverse(out);
        return out;
    }

    public void clear() {
        try {
            if (enabled && Files.exists(file)) {
                Files.delete(file);
            }
        } catch (IOException ignored) {
        }
    }

    // minimal hand-rolled JSON writer (dependency-free)
    private String toJson(Map<String, String> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : m.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(escape(e.getKey())).append("\":\"")
              .append(escape(e.getValue())).append('"');
            first = false;
        }
        return sb.append('}').toString();
    }

    private Map<String, String> fromJson(String line) {
        Map<String, String> m = new LinkedHashMap<>();
        String body = line.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length() - 1);

        boolean inValue = false;
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                if (inValue) {
                    m.put(key.toString(), unescape(value.toString()));
                    key.setLength(0);
                    value.setLength(0);
                    inValue = false;
                } else {
                    inValue = true;
                }
            } else if (c == ':' && !inValue) {
            } else if (inValue) {
                value.append(c);
            } else {
                key.append(c);
            }
        }
        return m;
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}