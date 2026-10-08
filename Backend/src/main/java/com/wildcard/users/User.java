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

    // @Builder.Default OLMAZSA builder() bu deyerleri null buraxir ve
    // NOT NULL constraint invoice-nu pozur (app start olmur).
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

    /** Son fəaliyyət anı (TOUCH GRASS achievement-i bundan hesablanır). */
    @Builder.Default
    @Column(name = "last_active_at")
    private java.time.Instant lastActiveAt = java.time.Instant.now();

    /** Has the user finished the "what do you like?" step after registering? */
    @Builder.Default
    @Column(name = "is_onboarded", nullable = false)
    private boolean onboarded = false;

    /**
     * java.security.Principal - STOMP istifadəçi mənbəyi üçün sabit ad.
     * 
     * {@code /user/{ad}/queue/alerts} göndərişləri bu dəyərə uyğun olmalıdır,
     * ona görə də id istifadə edirik (sendMessage tərəfi də {@code senderId}
     * göndərir). Object.toString() kimi dəyişən (identity hash) ad işləməzdi.
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
        // heç bir yaradıcı yol (builder/new/setter) unudanda da NULL düşməsin
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
