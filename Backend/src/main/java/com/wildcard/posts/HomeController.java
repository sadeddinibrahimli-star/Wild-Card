package com.wildcard.posts;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.PageResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.posts.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ANA SƏİFƏ.
 *
 * Sıralama: istifadəçinin mövzu affinity-si > paylaşım sayı > yenilik.
 * Mənbə: bəyənilmiş mövzuların postları + izlədiyi adamlar + öz poçtları.
 */
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final PostService postService;

    @GetMapping("/feed")
    public ApiResponse<PageResponse<PostResponse>> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort) {

        return ApiResponse.success(postService.homeFeed(
                SecurityUtils.getCurrentUser(), PageRequest.of(page, size), sort));
    }
}