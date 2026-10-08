package com.wildcard.music;

import com.wildcard.common.BusinessException;
import com.wildcard.common.ContentSanitizer;
import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpService;
import com.wildcard.music.dto.MusicArcReactionSummaryResponse;
import com.wildcard.music.dto.MusicCheckInRequest;
import com.wildcard.music.dto.MusicCheckInResponse;
import com.wildcard.reactions.ReactionType;
import com.wildcard.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Daily check-ins and arc reactions. */
@Service
@RequiredArgsConstructor
public class MusicArcEngagementService {

    private final MusicArcRepository musicArcRepository;
    private final MusicArcCheckInRepository checkInRepository;
    private final MusicArcReactionRepository reactionRepository;
    private final XpService xpService;

    @Transactional
    public MusicCheckInResponse checkIn(User user, MusicCheckInRequest request) {
        MusicArc arc = musicArcRepository
                .findByUserIdAndCurrentTrue(user.getId())
                .orElseThrow(() -> new BusinessException("Set a current music arc first"));

        LocalDate today = LocalDate.now();

        boolean already = checkInRepository.findByArcIdAndCheckInDate(arc.getId(), today).isPresent();
        if (!already) {
            checkInRepository.save(MusicArcCheckIn.builder()
                    .arc(arc)
                    .checkInDate(today)
                    .note(ContentSanitizer.text(request == null ? null : request.getNote()))
                    .createdAt(LocalDateTime.now())
                    .build());
            xpService.grant(user, XpAction.MUSIC_CHECK_IN);
        }

        List<LocalDate> dates = checkInRepository.checkInDatesFor(user.getId());
        return MusicCheckInResponse.builder()
                .arcId(arc.getId())
                .trackName(arc.getTrackName())
                .artist(arc.getArtist())
                .date(today)
                .alreadyCheckedIn(already)
                .streakDays(streak(dates))
                .totalCheckIns((int) checkInRepository.countByArcIdAndCheckInDateGreaterThanEqual(
                        arc.getId(), LocalDate.of(1970, 1, 1)))
                .message(already ? "Already checked in today" : "Checked in")
                .build();
    }

    /** The dates are DESC: the streak must start today or yesterday. */
    private int streak(List<LocalDate> datesDesc) {
        if (datesDesc == null || datesDesc.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate expected = datesDesc.get(0).equals(today) ? today : today.minusDays(1);
        int streak = 0;
        for (LocalDate d : datesDesc) {
            if (d.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (streak == 0 && d.isBefore(today)) {
                // does not continue a past streak
                break;
            }
        }
        return streak;
    }

    /** Pressing the same reaction again removes it (toggle). */
    @Transactional
    public MusicArcReactionSummaryResponse toggleReaction(User user, Long arcId, ReactionType type) {
        MusicArc arc = requireArc(arcId);
        var existing = reactionRepository.findByArcIdAndUserId(arcId, user.getId());

        if (existing.isPresent() && existing.get().getType() == type) {
            reactionRepository.delete(existing.get());
        } else if (existing.isPresent()) {
            existing.get().setType(type);
            reactionRepository.save(existing.get());
        } else {
            reactionRepository.save(MusicArcReaction.builder()
                    .arc(arc)
                    .user(user)
                    .type(type)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return summary(user, arcId);
    }

    @Transactional(readOnly = true)
    public MusicArcReactionSummaryResponse reactionsOf(User user, Long arcId) {
        return summary(user, arcId);
    }

    private MusicArcReactionSummaryResponse summary(User user, Long arcId) {
        List<MusicArcReaction> all = reactionRepository.findByArcId(arcId);
        Map<ReactionType, Integer> counts = new LinkedHashMap<>();
        ReactionType mine = null;
        for (MusicArcReaction r : all) {
            counts.merge(r.getType(), 1, Integer::sum);
            if (r.getUser().getId().equals(user.getId())) {
                mine = r.getType();
            }
        }
        return MusicArcReactionSummaryResponse.builder()
                .arcId(arcId)
                .total(all.size())
                .counts(counts)
                .myReaction(mine)
                .build();
    }

    private MusicArc requireArc(Long arcId) {
        return musicArcRepository.findById(arcId)
                .orElseThrow(() -> new BusinessException("Music arc not found"));
    }

    /**
     * Check-in stats of the user - part of the /music/current response so
     * the frontend does not need an extra request.
     */
    @Transactional(readOnly = true)
    public long[] checkInStats(Long userId) {
        List<java.time.LocalDateTime> starts =
                musicArcRepository.startDatesFor(userId);
        if (starts == null || starts.isEmpty()) {
            return new long[]{0L, 0L, 0L};   // streak, today, total
        }

        var checkInRepository2 = checkInRepository;
        List<MusicArcCheckIn> all = checkInRepository2.findAll().stream()
                .filter(c -> c.getArc().getUser().getId().equals(userId))
                .sorted((a, b) -> b.getCheckInDate().compareTo(a.getCheckInDate()))
                .toList();

        List<java.time.LocalDate> dates = all.stream()
                .map(MusicArcCheckIn::getCheckInDate)
                .toList();

        int streak = streak(dates);
        boolean today = dates.contains(java.time.LocalDate.now());
        return new long[]{streak, today ? 1 : 0, dates.size()};
    }
}
