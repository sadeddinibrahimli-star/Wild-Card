package com.wildcard.admin;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.NotFoundException;
import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpConfigRepository;
import com.wildcard.gamification.XpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/xp-config")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class XpConfigController {

    private final XpService xpService;
    private final XpConfigRepository xpConfigRepository;

    @GetMapping
    public ApiResponse<List<XpConfigEntry>> list() {
        return ApiResponse.success(Arrays.stream(XpAction.values())
                .map(action -> new XpConfigEntry(action.name(), xpService.valueOf(action)))
                .toList());
    }

    @PutMapping("/{action}")
    @Transactional
    public ApiResponse<List<XpConfigEntry>> update(@PathVariable XpAction action,
                                                    @Valid @RequestBody UpdateRequest request) {
        var config = xpConfigRepository.findByAction(action)
                .orElseGet(() -> xpConfigRepository.save(com.wildcard.gamification.XpConfig.builder()
                        .action(action)
                        .value(action.getDefaultValue())
                        .build()));

        config.setValue(request.getValue());
        xpConfigRepository.save(config);
        xpService.reloadConfig();

        return list();
    }

    public record XpConfigEntry(String action, int value) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        @NotNull
        @Min(0)
        private Integer value;
    }
}
