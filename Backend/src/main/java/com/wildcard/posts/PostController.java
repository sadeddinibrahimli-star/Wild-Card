package com.wildcard.posts;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.posts.dto.CreatePostRequest;
import com.wildcard.posts.dto.PostResponse;
import com.wildcard.posts.dto.UpdatePostRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    public ApiResponse<PostResponse> create(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success("Post created",
                postService.create(SecurityUtils.getCurrentUser(), request));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<PostResponse> get(@PathVariable Long postId) {
        return ApiResponse.success(postService.get(postId, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<PostResponse> update(@PathVariable Long postId,
                                            @Valid @RequestBody UpdatePostRequest request) {
        return ApiResponse.success("Post updated",
                postService.update(SecurityUtils.getCurrentUser(), postId, request));
    }

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long postId) {
        postService.softDelete(SecurityUtils.getCurrentUser(), postId);
    }

    @GetMapping("/posts")
    public ApiResponse<PageResponse<PostResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Category parsed = category == null ? null : Category.from(category);

        return ApiResponse.success(postService.list(parsed, sorted(page, size), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/users/{userId}/posts")
    public ApiResponse<PageResponse<PostResponse>> listByAuthor(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(postService.listByAuthor(userId, sorted(page, size), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/feed")
    public ApiResponse<PageResponse<PostResponse>> feed(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean onlyFollowing,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(postService.feed(
                SecurityUtils.getCurrentUser(),
                PageRequest.of(page, size),
                PostService.FeedSort.from(sort),
                category == null ? null : Category.from(category),
                onlyFollowing));
    }

    private PageRequest sorted(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}