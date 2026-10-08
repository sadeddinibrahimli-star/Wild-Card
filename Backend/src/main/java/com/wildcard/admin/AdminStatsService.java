package com.wildcard.admin;

import com.wildcard.common.enums.AccountStatus;
import com.wildcard.comments.CommentRepository;
import com.wildcard.gamification.XpLogRepository;
import com.wildcard.moderation.ReportRepository;
import com.wildcard.moderation.ReportStatus;
import com.wildcard.posts.PostRepository;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final XpLogRepository xpLogRepository;
    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> platformStats() {
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        // activeUsers is not the account status: REAL activity in the last 24 hours
        stats.put("activeUsers", userRepository.countActiveInLast24Hours(java.time.Instant.now().minusSeconds(86400)));
        stats.put("activeUsersAccountStatus", userRepository.countByAccountStatus(AccountStatus.ACTIVE));
        stats.put("suspendedUsers", userRepository.countByAccountStatus(AccountStatus.SUSPENDED));
        stats.put("restrictedUsers", userRepository.countByAccountStatus(AccountStatus.RESTRICTED));
        stats.put("totalPosts", postRepository.countByDeletedFalseAndHiddenByModerationFalse());
        stats.put("postsToday", postRepository.countByDeletedFalseAndHiddenByModerationFalseAndCreatedAtAfter(startOfToday));
        stats.put("totalComments", commentRepository.count());
        stats.put("totalXpGranted", xpLogRepository.totalXpGranted());
        stats.put("xpGrantedThisWeek", xpLogRepository.totalXpGrantedSince(weekAgo));
        stats.put("pendingReports", reportRepository.countByStatus(ReportStatus.PENDING));
        stats.put("postsPerDay", postsPerDay());
        return stats;
    }

    /** Last 7 days of posts, one entry per day, oldest first. Missing days are zero filled. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> postsPerDay() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);

        Map<String, Long> counted = new LinkedHashMap<>();
        for (Object[] row : postRepository.countPerDaySince(from.atStartOfDay())) {
            counted.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = from.plusDays(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", day.toString());
            entry.put("label", day.getDayOfWeek().name().substring(0, 3));
            entry.put("count", counted.getOrDefault(day.toString(), 0L));
            out.add(entry);
        }
        return out;
    }
}
