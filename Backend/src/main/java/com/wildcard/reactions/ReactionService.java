package com.wildcard.reactions;

import com.wildcard.common.BusinessException;
import com.wildcard.common.NotFoundException;
import com.wildcard.common.SecurityUtils;
import com.wildcard.gamification.XpAction;
import com.wildcard.gamification.XpService;
import com.wildcard.notifications.NotificationService;
import com.wildcard.notifications.NotificationType;
import com.wildcard.posts.Post;
import com.wildcard.posts.PostRepository;
import com.wildcard.reactions.dto.ReactionSummaryResponse;
import com.wildcard.topics.TopicAffinityService;
import com.wildcard.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final XpService xpService;
    private final NotificationService notificationService;
    private final TopicAffinityService topicAffinityService;

    @Transactional
    public ReactionSummaryResponse react(User current, Long postId, ReactionType type) {
        SecurityUtils.requireCanPublish(current);

        Post post = postRepository.findByIdAndDeletedFalseAndHiddenByModerationFalse(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        if (post.getAuthor().getId().equals(current.getId())) {
            throw new BusinessException("You cannot react to your own post");
        }

        Reaction reaction = reactionRepository.findByPostIdAndUserId(postId, current.getId())
                .orElse(null);

        if (reaction != null && reaction.getType() == type) {
            // eyni tipə ikinci basma = geri götür
            reactionRepository.delete(reaction);
            topicAffinityService.registerReaction(current, post);
        } else if (reaction != null) {
            reaction.setType(type);
            reactionRepository.save(reaction);

            // reaction type change olunanda da movzu cekisi artir
            // (her eylem istifadecinin maraqini gosterir)
            topicAffinityService.registerReaction(current, post);
        } else {
            reactionRepository.save(Reaction.builder()
                    .post(post)
                    .user(current)
                    .type(type)
                    .build());

            User author = post.getAuthor();
            xpService.grant(author, XpAction.REACTION_RECEIVED, post.getCategory().getPrimaryStat());

            notificationService.notify(author, NotificationType.REACTION,
                    current.getUsername() + " reacted to your post", post.getId());

            topicAffinityService.registerReaction(current, post);
        }

        return summary(postId, current);
    }

    @Transactional
    public void remove(User current, Long postId) {
        Reaction reaction = reactionRepository.findByPostIdAndUserId(postId, current.getId())
                .orElseThrow(() -> new BusinessException("You have not reacted to this post"));

        reactionRepository.delete(reaction);
    }

    @Transactional(readOnly = true)
    public ReactionSummaryResponse summary(Long postId, User current) {
        Reaction mine = current == null ? null
                : reactionRepository.findByPostIdAndUserId(postId, current.getId()).orElse(null);

        Map<String, Long> counts = countByType(postId);

        return ReactionSummaryResponse.builder()
                .myReaction(mine == null ? null : mine.getType().name())
                .total(counts.values().stream().mapToLong(Long::longValue).sum())
                .countsByType(counts)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countByType(Long postId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReactionType type : ReactionType.values()) {
            counts.put(type.name(), 0L);
        }

        for (Object[] row : reactionRepository.countByTypeForPost(postId)) {
            counts.put(((ReactionType) row[0]).name(), ((Number) row[1]).longValue());
        }

        return counts;
    }
}