package com.wildcard.achievements;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserId(Long userId);

    Optional<UserAchievement> findByUserIdAndCode(Long userId, String code);

    boolean existsByUserIdAndCode(Long userId, String code);

    long countByUserId(Long userId);
}
