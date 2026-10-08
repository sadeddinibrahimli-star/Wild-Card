package com.wildcard.topics;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.SecurityUtils;
import com.wildcard.posts.Category;
import com.wildcard.topics.dto.SetTopicsRequest;
import com.wildcard.topics.dto.TopicResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TopicController {

    private final TopicRepository topicRepository;
    private final TopicAffinityService affinityService;

    @GetMapping("/topics")
    public ApiResponse<List<TopicResponse>> browse(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category) {

        List<Topic> topics;
        if (q != null && !q.isBlank()) {
            topics = topicRepository.findTop50ByLabelContainingIgnoreCaseOrderByLabelAsc(q);
        } else if (category != null && !category.isBlank()) {
            topics = topicRepository.findByCategoryOrderByLabelAsc(Category.from(category));
        } else {
            topics = topicRepository.findAll();
        }

        return ApiResponse.success(topics.stream()
                .map(topic -> TopicResponse.builder()
                        .id(topic.getId())
                        .slug(topic.getSlug())
                        .label(topic.getLabel())
                        .category(topic.getCategory().name())
                        .affinity(0.0)
                        .explicit(false)
                        .build())
                .toList());
    }

    @GetMapping("/users/me/topics")
    public ApiResponse<List<TopicResponse>> myTopics() {
        return ApiResponse.success(affinityService.affinitiesOf(SecurityUtils.getCurrentUser()));
    }

    @PutMapping("/users/me/topics")
    public ApiResponse<List<TopicResponse>> setMyTopics(@Valid @RequestBody SetTopicsRequest request) {
        return ApiResponse.success("Topics updated",
                affinityService.setExplicitTopics(SecurityUtils.getCurrentUser(), request.getTopicIds()));
    }

    @GetMapping("/topics/{id}")
    public ApiResponse<TopicResponse> get(@PathVariable Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new com.wildcard.common.NotFoundException("Topic not found: " + id));

        return ApiResponse.success(TopicResponse.builder()
                .id(topic.getId())
                .slug(topic.getSlug())
                .label(topic.getLabel())
                .category(topic.getCategory().name())
                .affinity(0.0)
                .explicit(false)
                .build());
    }
}