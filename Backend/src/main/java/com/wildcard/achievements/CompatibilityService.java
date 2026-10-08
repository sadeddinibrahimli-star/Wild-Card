package com.wildcard.achievements;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wildcard.achievements.dto.CompatibilityResponse;
import com.wildcard.common.NotFoundException;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompatibilityService {

    private static final Cache<String, CompatibilityResponse> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(5_000)
            .build();

    private final UserRepository userRepository;

    public CompatibilityResponse between(Long userId, Long otherId) {
        String key = userId + ":" + otherId;
        CompatibilityResponse cached = CACHE.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        CompatibilityResponse computed = compute(userId, otherId);
        CACHE.put(key, computed);
        return computed;
    }

    public void invalidate(Long userId) {
        CACHE.asMap().keySet().removeIf(k -> k.startsWith(userId + ":") || k.endsWith(":" + userId));
    }

    @Transactional(readOnly = true)
    protected CompatibilityResponse compute(Long userId, Long otherId) {
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        User other = userRepository.findById(otherId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<CompatibilityResponse.StatPair> stats = new ArrayList<>();
        int total = 0;

        for (Object[] row : new Object[][] {
                {"ANI", "Anime", me.getAni(), other.getAni()},
                {"GAM", "Gaming", me.getGam(), other.getGam()},
                {"MUS", "Music", me.getMus(), other.getMus()},
                {"CHA", "Chaos", me.getCha(), other.getCha()},
        }) {
            int a = (Integer) row[2];
            int b = (Integer) row[3];
            int score = Math.max(0, 100 - Math.abs(a - b));
            total += score;

            stats.add(CompatibilityResponse.StatPair.builder()
                    .key((String) row[0])
                    .label((String) row[1])
                    .mine(a)
                    .theirs(b)
                    .score(score)
                    .build());
        }

        return CompatibilityResponse.builder()
                .userId(me.getId())
                .otherId(other.getId())
                .otherUsername(other.getUsername())
                .otherAvatarUrl(other.getAvatarUrl())
                .overall(Math.round(total / 4f))
                .score(Math.round(total / 4f))
                .anime(pairsValue(stats, "ANI"))
                .gaming(pairsValue(stats, "GAM"))
                .music(pairsValue(stats, "MUS"))
                .chaos(pairsValue(stats, "CHA"))
                .stats(stats)
                .build();
    }

    private int pairsValue(List<CompatibilityResponse.StatPair> stats, String key) {
        return stats.stream()
                .filter(s -> s.getKey().equals(key))
                .mapToInt(CompatibilityResponse.StatPair::getScore)
                .findFirst()
                .orElse(0);
    }
}