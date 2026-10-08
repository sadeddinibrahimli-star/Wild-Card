package com.wildcard.posts;

import com.wildcard.common.BusinessException;
import com.wildcard.common.ContentSanitizer;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.PageResponse;
import com.wildcard.common.RateLimiter;
import com.wildcard.common.SecurityUtils;
import com.wildcard.comments.CommentService;
import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpService;
import com.wildcard.posts.dto.CreatePostRequest;
import com.wildcard.posts.dto.PostResponse;
import com.wildcard.posts.dto.UpdatePostRequest;
import com.wildcard.reactions.ReactionRepository;
import com.wildcard.reactions.ReactionService;
import com.wildcard.social.FollowService;
import com.wildcard.topics.TopicRepository;
import com.wildcard.topics.TopicAffinityService;
import com.wildcard.users.enums.Role;
import com.wildcard.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final XpService xpService;
    private final RateLimiter rateLimiter;
    private final CommentService commentService;
    private final ReactionService reactionService;
    private final ReactionRepository reactionRepository;
    private final FollowService followService;
    private final TopicAffinityService topicAffinityService;
    private final TopicRepository topicRepository;

    @Transactional
    public PostResponse create(User author, CreatePostRequest request) {
        SecurityUtils.requireCanPublish(author);
        rateLimiter.checkPosts(author);

        Post post = new Post();
        post.setAuthor(author);
        post.setCategory(request.getCategory());
        post.setTitle(ContentSanitizer.line(request.getTitle(), 200));
        post.setBody(ContentSanitizer.richText(request.getBody()));
        post.setImageUrl(ContentSanitizer.line(request.getImageUrl(), 255));

        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            post.getTopics().addAll(topicRepository.findAllById(request.getTopicIds()));
        }

        post = postRepository.save(post);

        xpService.grant(author, XpAction.POST, request.getCategory().getPrimaryStat());

        return toResponse(post);
    }

    @Transactional
    public PostResponse update(User current, Long postId, UpdatePostRequest request) {
        Post post = getActive(postId);
        requireOwnerOrStaff(current, post);

        if (request.getCategory() != null) {
            post.setCategory(request.getCategory());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            post.setTitle(ContentSanitizer.line(request.getTitle(), 200));
        }
        if (request.getBody() != null && !request.getBody().isBlank()) {
            post.setBody(ContentSanitizer.richText(request.getBody()));
        }
        if (request.getImageUrl() != null) {
            post.setImageUrl(ContentSanitizer.line(request.getImageUrl(), 255));
        }

        return toResponse(postRepository.save(post));
    }

    @Transactional
    public void softDelete(User current, Long postId) {
        Post post = getActive(postId);
        requireOwnerOrStaff(current, post);

        post.setDeleted(true);
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public Post getActive(Long postId) {
        return postRepository.findByIdAndDeletedFalseAndHiddenByModerationFalse(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long postId, Long viewerId) {
        return toResponse(getActive(postId), viewerId);
    }

    public PostResponse get(Long postId) {
        return toResponse(getActive(postId));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> list(Category category, Pageable pageable) {
        return list(category, pageable, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> list(Category category, Pageable pageable, Long viewerId) {
        var page = category == null
                ? postRepository.findByDeletedFalseAndHiddenByModerationFalseOrderByCreatedAtDesc(pageable)
                : postRepository.findByCategoryAndDeletedFalseAndHiddenByModerationFalseOrderByCreatedAtDesc(category, pageable);

        return PageResponse.from(page.map(p -> toResponse(p, viewerId)));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listByAuthor(Long authorId, Pageable pageable) {
        return listByAuthor(authorId, pageable, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listByAuthor(Long authorId, Pageable pageable, Long viewerId) {
        return PageResponse.from(
                postRepository.findByAuthorIdAndDeletedFalseAndHiddenByModerationFalseOrderByCreatedAtDesc(authorId, pageable)
                        .map(p -> toResponse(p, viewerId)));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> feed(User current, Pageable pageable, FeedSort sort) {
        return feed(current, pageable, sort, null, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> feed(User current, Pageable pageable, FeedSort sort,
                                          Category category, boolean onlyFollowing) {
        List<Long> followingIds = followService.followingIds(current.getId());

        List<Long> ids = followingIds.isEmpty()
                ? List.of(current.getId())
                : Stream.concat(followingIds.stream(), Stream.of(current.getId())).toList();

        if (category != null || onlyFollowing) {
            var filtered = postRepository.feedForFiltered(
                    current.getId(), ids, category, onlyFollowing, pageable);
            return PageResponse.from(filtered.map(p -> toResponse(p, current.getId())));
        }

        var page = sort == FeedSort.RELEVANCE
                ? postRepository.feedForByRelevance(current.getId(), ids, pageable)
                : postRepository.feedFor(current.getId(), ids, pageable);

        return PageResponse.from(page.map(p -> toResponse(p, current.getId())));
    }

    /**
     * HOME PAGE.
     *
     * Source: liked topics + followed people + own posts.
     * Ordering:
     *  sort=latest (default) -> newest post first, time based; affinity and
     *                           reactions only separate posts written at the
     *                           same time.
     *  sort=relevance        -> affinity first, then reactions (older behaviour).
     */
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> homeFeed(User current, Pageable pageable) {
        return homeFeed(current, pageable, "latest");
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> homeFeed(User current, Pageable pageable, String sort) {
        List<Long> followingIds = followService.followingIds(current.getId());
        List<Long> liked = new ArrayList<>(topicAffinityService.likedTopicIds(current.getId()));

        boolean relevance = "relevance".equalsIgnoreCase(sort);
        Page<Post> page = relevance
                ? postRepository.homeFeedByRelevance(current.getId(), followingIds, liked, pageable)
                : postRepository.homeFeed(current.getId(), followingIds, liked, pageable);

        List<Post> merged = new ArrayList<>(page.getContent());

        // A new user follows nobody and picked no topics, so the feed would only
        // contain their own posts. If the first page is still short we top it up
        // with the newest posts on the platform (doc 4.3).
        if (pageable.getOffset() == 0 && merged.size() < pageable.getPageSize()) {
            List<Long> shown = merged.stream().map(Post::getId).toList();
            if (shown.isEmpty()) {
                shown = List.of(current.getId());
            }
            int need = pageable.getPageSize() - merged.size();
            List<Post> extra = postRepository
                    .findRecentExcluding(shown, org.springframework.data.domain.PageRequest.of(0, need))
                    .getContent();
            merged.addAll(extra);
        }

        long total = page.getTotalElements() + (merged.size() - page.getContent().size());
        List<PostResponse> body = merged.stream()
                .map(p -> toResponse(p, current.getId()))
                .toList();

        return PageResponse.from(
                new org.springframework.data.domain.PageImpl<>(body, pageable, Math.max(total, body.size())));
    }

    private void requireOwnerOrStaff(User current, Post post) {
        boolean isOwner = post.getAuthor().getId().equals(current.getId());
        boolean isStaff = current.getRole() == Role.ADMIN || current.getRole() == Role.MODERATOR;

        if (!isOwner && !isStaff) {
            throw new BusinessException("You can only change your own posts");
        }
    }

    @Transactional(readOnly = true)
    /** myReaction is only filled when viewerId is known. */
    public PostResponse toResponse(Post post, Long viewerId) {
        PostResponse dto = toResponse(post);
        if (viewerId != null) {
            reactionRepository.findByPostIdAndUserId(post.getId(), viewerId)
                    .ifPresent(r -> dto.setMyReaction(r.getType().name()));
        }
        return dto;
    }

    public PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorUsername(post.getAuthor().getUsername())
                .authorLevel(post.getAuthor().getLevel())
                .authorRarity(post.getAuthor().getRarity())
                .authorAvatarUrl(post.getAuthor().getAvatarUrl())
                .category(post.getCategory())
                .title(post.getTitle())
                .body(post.getBody())
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .commentCount(commentService.countActiveFor(post.getId()))
                .reactionCounts(reactionService.countByType(post.getId()))
                .topics(post.getTopics() == null ? List.of() : post.getTopics().stream()
                        .map(topic -> new PostResponse.TopicDto(topic.getId(), topic.getSlug(), topic.getLabel()))
                        .toList())
                .build();
    }

    public enum FeedSort {
        RECENCY,
        RELEVANCE;

        public static FeedSort from(String value) {
            if (value == null || value.isBlank()) {
                return RECENCY;
            }
            // synonyms: the doc says "recency or relevance", the frontend sends "latest"
            return switch (value.toLowerCase()) {
                case "latest", "newest", "recent", "recency" -> RECENCY;
                case "relevance", "relevant" -> RELEVANCE;
                default -> throw new BusinessException(
                        "Unknown sort: " + value + ". Allowed: RECENCY, RELEVANCE (also: latest, relevance)");
            };
        }
    }
}