package com.wildcard.topics;

import com.wildcard.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bir istifadəçinin bir mövzuya "çəkisi".
 *
 * score necə artır:
 *   - istifadəçi bu mövzulu poçta REAKSİYA verdikdə  -> +reactionWeight
 *   - istifadəçi mövzuya özü SEÇDİ (settings)          -> explicit = true, score yüksək
 *
 * explicit = true olan mövzular hər zaman reytinqdə öndə çıxır.
 */
@Entity
@Table(name = "topic_affinity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_affinity_user_topic", columnNames = {"user_id", "topic_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicAffinity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false)
    private double score;

    @Column(name = "is_explicit", nullable = false)
    private boolean explicit;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}