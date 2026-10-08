package com.wildcard.gamification;

import com.wildcard.common.BusinessException;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserRepository userRepository;

    @Transactional
    public void recordActivity(User user) {
        LocalDate today = LocalDate.now();
        user.setLastActiveAt(java.time.Instant.now());

        if (today.equals(user.getLastActiveDate())) {
            userRepository.save(user);
            return;
        }

        if (today.minusDays(1).equals(user.getLastActiveDate())) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            user.setCurrentStreak(1);
        }

        user.setLastActiveDate(today);
        user.setLongestStreak(Math.max(user.getLongestStreak(), user.getCurrentStreak()));
        userRepository.save(user);
    }

    @Scheduled(cron = "0 15 0 * * *")
    @Transactional
    public void resetLapsedStreaks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        for (User user : userRepository.findAll()) {
            if (user.getCurrentStreak() > 0
                    && user.getLastActiveDate() != null
                    && user.getLastActiveDate().isBefore(yesterday)) {
                user.setCurrentStreak(0);
                userRepository.save(user);
            }
        }
    }
}