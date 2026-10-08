package com.wildcard.music;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.music.dto.MusicArcReactionRequest;
import com.wildcard.music.dto.MusicArcReactionSummaryResponse;
import com.wildcard.music.dto.MusicArcResponse;
import com.wildcard.music.dto.MusicCheckInRequest;
import com.wildcard.music.dto.MusicCheckInResponse;
import com.wildcard.music.dto.UpdateMusicArcRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/music")
@RequiredArgsConstructor
public class MusicArcController {

    private final MusicArcService musicArcService;
    private final MusicArcEngagementService engagementService;

    @GetMapping("/current")
    public ApiResponse<MusicArcResponse> current() {
        return ApiResponse.success(musicArcService.getCurrent(SecurityUtils.getCurrentUser()));
    }

    // frontend hər iki verb-i də istifadə edir.
    // ƏVVƏLCİ bu iki metod ayrı-ayrı idi -> eyni path-ə iki PUT mapping
    // düşürdü: "Ambiguous handler methods mapped for /music/current"
    // və PUT /music/current həmişə 500 qaytarırdı. İndi tək metoddur.
    @RequestMapping(value = "/current", method = {RequestMethod.PUT, RequestMethod.POST})
    public ApiResponse<MusicArcResponse> saveCurrent(@Valid @RequestBody UpdateMusicArcRequest request) {
        return ApiResponse.success("Music arc updated",
                musicArcService.setCurrent(SecurityUtils.getCurrentUser(), request));
    }

    @GetMapping("/arcs")
    public ApiResponse<PageResponse<MusicArcResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(
                musicArcService.history(SecurityUtils.getCurrentUser(), PageRequest.of(page, size)));
    }

    // ---------------- check-in ----------------

    // mövcud yol qalır: POST /music/arc/checkin
    @PostMapping({"/arc/checkin", "/checkin"})
    public ApiResponse<MusicCheckInResponse> checkIn(
            @Valid @RequestBody(required = false) MusicCheckInRequest request) {
        return ApiResponse.success(
                engagementService.checkIn(SecurityUtils.getCurrentUser(), request));
    }

    // ---------------- arc reaksiyaları ----------------

    // POST qalır, PUT də əlavə olunur (frontend sözleşməsi)
    @RequestMapping(value = "/arcs/{id}/reaction", method = {RequestMethod.POST, RequestMethod.PUT})
    public ApiResponse<MusicArcReactionSummaryResponse> react(
            @PathVariable Long id,
            @Valid @RequestBody MusicArcReactionRequest request) {

        return ApiResponse.success(
                engagementService.toggleReaction(SecurityUtils.getCurrentUser(), id, request.getType()));
    }

    @GetMapping("/arcs/{id}/reactions")
    public ApiResponse<MusicArcReactionSummaryResponse> reactions(@PathVariable Long id) {
        return ApiResponse.success(engagementService.reactionsOf(SecurityUtils.getCurrentUser(), id));
    }
}