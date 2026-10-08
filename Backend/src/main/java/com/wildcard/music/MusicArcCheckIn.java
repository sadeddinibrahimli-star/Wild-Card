package com.wildcard.music;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A daily "check-in" on a MusicArc.
 * Unique (arc_id, check_in_date) -> once per day.
 */
@Entity
@Table(name = "music_arc_checkins",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_checkin_arc_date", columnNames = {"arc_id", "check_in_date"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicArcCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "arc_id", nullable = false)
    private MusicArc arc;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "note", length = 280)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}