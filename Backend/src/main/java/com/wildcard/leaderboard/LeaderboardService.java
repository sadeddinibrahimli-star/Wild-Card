package com.wildcard.leaderboard;

import com.wildcard.common.PageResponse;
import com.wildcard.gamification.XpLogRepository;
import com.wildcard.leaderboard.dto.LeaderboardRowResponse;
import com.wildcard.leaderboard.dto.WeeklyPositionResponse;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final XpLogRepository xpLogRepository;
    private final UserRepository userRepository;

    private volatile List<LeaderboardRowResponse> weeklyCache = List.of();
    private volatile LocalDateTime calculatedAt;

    @Transactional(readOnly = true)
    public void recalculate() {
        List<Object[]> rows = xpLogRepository.weeklyRanking(LocalDateTime.now().minusDays(7));

        List<Long> userIds = rows.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        Map<Long, User> users = userIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<LeaderboardRowResponse> ranking = new ArrayList<>(rows.size());
        int rank = 0;

        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            long weeklyXp = ((Number) row[2]).longValue();
            User user = users.get(userId);
            rank++;

            ranking.add(LeaderboardRowResponse.builder()
                    .rank(rank)
                    .userId(userId)
                    .username((String) row[1])
                    .avatarUrl(user == null ? null : user.getAvatarUrl())
                    .rarity(user == null ? null : user.getRarity())
                    .title(user == null ? null : user.getTitle())
                    .weeklyXp(weeklyXp)
                    .build());
        }

        this.weeklyCache = List.copyOf(ranking);
        this.calculatedAt = LocalDateTime.now();
        log.info("Leaderboard recalculated: {} ranked users", ranking.size());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardRowResponse> top(int limit) {
        ensureCalculated();
        return weeklyCache.stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaderboardRowResponse> page(Pageable pageable) {
        ensureCalculated();
        return PageResponse.from(new PageImpl<>(weeklyCache, pageable, weeklyCache.size()));
    }

    @Transactional(readOnly = true)
    public WeeklyPositionResponse myPosition(User user) {
        ensureCalculated();

        return weeklyCache.stream()
                .filter(row -> row.getUserId().equals(user.getId()))
                .findFirst()
                .map(row -> WeeklyPositionResponse.builder()
                        .ranked(true)
                        .rank(row.getRank())
                        .weeklyXp(row.getWeeklyXp())
                        .totalXp(user.getTotalXp())
                        .build())
                .orElseGet(() -> WeeklyPositionResponse.builder()
                        .ranked(false)
                        .rank(0)
                        .weeklyXp(0)
                        .totalXp(user.getTotalXp())
                        .build());
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    private void ensureCalculated() {
        if (weeklyCache.isEmpty()) {
            recalculate();
        }
    }
}
