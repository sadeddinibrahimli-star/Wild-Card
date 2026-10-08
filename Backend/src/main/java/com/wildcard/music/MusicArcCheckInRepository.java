package com.wildcard.music;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MusicArcCheckInRepository extends JpaRepository<MusicArcCheckIn, Long> {

    Optional<MusicArcCheckIn> findByArcIdAndCheckInDate(Long arcId, LocalDate date);

    long countByArcIdAndCheckInDateGreaterThanEqual(Long arcId, LocalDate from);

    List<MusicArcCheckIn> findByArcIdOrderByCheckInDateDesc(Long arcId);

    /**
     * İstifadəçinin bütün check-in tarixləri - streak hesablanır.
     */
    @Query("select c.checkInDate from MusicArcCheckIn c where c.arc.user.id = :userId order by c.checkInDate desc")
    List<LocalDate> checkInDatesFor(@Param("userId") Long userId);
}
