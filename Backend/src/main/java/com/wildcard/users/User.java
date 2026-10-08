package com.wildcard.users;

import com.wildcard.common.enums.AccountStatus;
import com.wildcard.users.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements java.security.Principal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String email;

    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(40)")
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(40)")
    private Role role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int ani;

    @Column(nullable = false)
    private int gam;

    @Column(nullable = false)
    private int mus;

    @Column(nullable = false)
    private int cha;

    @Column(nullable = false)
    private long totalXp;

    @Column(nullable = false)
    private int level;

    // Without @Builder.Default the builder() would leave these values null and
    // break the NOT NULL constraints (the app would not start).
    @Builder.Default
    @Column(nullable = false)
    private String rarity = "COMMON";

    @Builder.Default
    @Column(nullable = false)
    private String title = "Wild Card Rookie";

    @Column(nullable = false)
    private int currentStreak;

    @Column(nullable = false)
    private int longestStreak;

    private LocalDate lastActiveDate;

    /** Last activity timestamp (used by the TOUCH GRASS achievement). */
    @Builder.Default
    @Column(name = "last_active_at")
    private java.time.Instant lastActiveAt = java.time.Instant.now();

    /** Has the user finished the "what do you like?" step after registering? */
    @Builder.Default
    @Column(name = "is_onboarded", nullable = false)
    private boolean onboarded = false;

    /**
     * Stable name for java.security.Principal, used as the STOMP user identity.
     *
     * {@code /user/{name}/queue/alerts} sends must match this value, so the id
     * is used (the send side also sends {@code senderId}). A changing
     * Object.toString() (identity hash) would not work.
     */
    @Override
    public String getName() {
        if (id != null) {
            return String.valueOf(id);
        }
        return username != null ? username : "anonymous";
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        // so no construction path (builder/new/setter) can leave it NULL
        if (rarity == null || rarity.isBlank()) {
            rarity = "COMMON";
        }
        if (title == null || title.isBlank()) {
            title = "Wild Card Rookie";
        }
        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }
        if (role == null) {
            role = Role.USER;
        }
        if (level <= 0) {
            level = 1;
        }
    }
}
