package com.wildcard.music;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "music_arcs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicArc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String artist;

    @Column(nullable = false, length = 255)
    private String trackName;

    @Column(length = 500)
    private String note;

    /** Albom qabığı (iTunes artworkUrl100). */
    @Column(name = "album_art_url", length = 500)
    private String albumArtUrl;

    @Builder.Default
    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
