package com.wildcard.reactions;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.reactions.dto.ReactionSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts/{postId}/reaction")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PutMapping
    public ApiResponse<ReactionSummaryResponse> react(
            @PathVariable Long postId,
            @Valid @RequestBody ReactionRequest request) {

        return ApiResponse.success("Reaction saved",
                reactionService.react(SecurityUtils.getCurrentUser(), postId, request.type()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long postId) {
        reactionService.remove(SecurityUtils.getCurrentUser(), postId);
    }

    @GetMapping
    public ApiResponse<ReactionSummaryResponse> summary(@PathVariable Long postId) {
        return ApiResponse.success(reactionService.summary(postId, SecurityUtils.getCurrentUser()));
    }

    public record ReactionRequest(ReactionType type) {
    }
}