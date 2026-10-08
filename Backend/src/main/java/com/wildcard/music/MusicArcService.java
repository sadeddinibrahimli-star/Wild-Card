package com.wildcard.music;

import com.wildcard.common.ContentSanitizer;
import com.wildcard.common.PageResponse;
import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpService;
import com.wildcard.music.dto.MusicArcResponse;
import com.wildcard.music.dto.UpdateMusicArcRequest;
import com.wildcard.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MusicArcService {

    private final MusicArcRepository musicArcRepository;
    private final XpService xpService;
    private final MusicArcEngagementService engagementService;

    @Transactional
    public MusicArcResponse setCurrent(User user, UpdateMusicArcRequest request) {
        musicArcRepository.archiveCurrent(user.getId(), LocalDateTime.now());

        MusicArc saved = musicArcRepository.save(MusicArc.builder()
                .user(user)
                .artist(ContentSanitizer.line(request.getArtist(), 255))
                .trackName(ContentSanitizer.line(request.getTrackName(), 255))
                .note(ContentSanitizer.text(request.getNote()))
                .albumArtUrl(request.getAlbumArtUrl())
                .current(true)
                .build());

        xpService.grant(user, XpAction.MUSIC_ARC_UPDATE);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public MusicArcResponse getCurrent(User user) {
        MusicArc arc = musicArcRepository.findByUserIdAndCurrentTrue(user.getId()).orElse(null);
        if (arc == null) {
            return null;
        }
        long[] stats = engagementService.checkInStats(user.getId());
        MusicArcResponse out = toResponse(arc);
        out.setStreakDays((int) stats[0]);
        out.setCheckedInToday(stats[1] == 1L);
        out.setTotalCheckIns(stats[2]);
        return out;
    }

    @Transactional(readOnly = true)
    public PageResponse<MusicArcResponse> history(User user, Pageable pageable) {
        return PageResponse.from(
                musicArcRepository.findByUserIdOrderByStartedAtDesc(user.getId(), pageable)
                        .map(this::toResponse));
    }

    private MusicArcResponse toResponse(MusicArc arc) {
        return MusicArcResponse.builder()
                .id(arc.getId())
                .artist(arc.getArtist())
                .trackName(arc.getTrackName())
                .note(arc.getNote())
                .albumArtUrl(arc.getAlbumArtUrl())
                .current(arc.isCurrent())
                .startedAt(arc.getStartedAt())
                .endedAt(arc.getEndedAt())
                .build();
    }
}