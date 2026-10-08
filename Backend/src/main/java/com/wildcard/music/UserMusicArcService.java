package com.wildcard.music;

import com.wildcard.music.dto.UserMusicArcResponse;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserMusicArcService {

    private final MusicArcRepository musicArcRepository;
    private final MusicArcCheckInRepository checkInRepository;
    private final MusicArcReactionRepository reactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserMusicArcResponse of(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.wildcard.common.BusinessException("User not found"));

        MusicArc arc = musicArcRepository.findByUserIdAndCurrentTrue(userId).orElse(null);
        if (arc == null) {
            return UserMusicArcResponse.builder().userId(userId).username(user.getUsername()).build();
        }

        List<MusicArcCheckIn> checkIns = checkInRepository.findByArcIdOrderByCheckInDateDesc(arc.getId());
        List<LocalDate> dates = checkIns.stream().map(MusicArcCheckIn::getCheckInDate).toList();
        LocalDate today = LocalDate.now();

        return UserMusicArcResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .arcId(arc.getId())
                .artist(arc.getArtist())
                .trackName(arc.getTrackName())
                .albumArtUrl(arc.getAlbumArtUrl())
                .note(arc.getNote())
                .startedOn(arc.getStartedAt() != null ? arc.getStartedAt().toLocalDate() : null)
                .checkedInToday(dates.contains(today))
                .streakDays(streak(dates))
                .recentCheckIns(dates.stream().limit(14).toList())
                .reactionCount(reactionRepository.findByArcId(arc.getId()).size())
                .build();
    }

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
            } else {
                break;
            }
        }
        return streak;
    }
}
