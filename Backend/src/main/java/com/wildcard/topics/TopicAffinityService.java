package com.wildcard.topics;

import com.wildcard.posts.Category;
import com.wildcard.posts.Post;
import com.wildcard.topics.dto.TopicResponse;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TopicAffinityService {

    private final TopicRepository topicRepository;
    private final TopicAffinityRepository affinityRepository;
    private final UserRepository userRepository;

    @Value("${wildcard.affinity.reaction-weight:1.0}")
    private double reactionWeight;

    @Value("${wildcard.affinity.threshold:5.0}")
    private double threshold;

    @Value("${wildcard.affinity.explicit-score:10.0}")
    private double explicitScore;

    /**
     * Reacting to a post raises the topic weight.
     * +reactionWeight per reaction; once the threshold is reached the topic
     * counts as "liked".
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registerReaction(User user, Post post) {
        if (post.getTopics() == null || post.getTopics().isEmpty()) {
            return;
        }

        for (Topic topic : post.getTopics()) {
            TopicAffinity affinity = affinityRepository
                    .findByUserIdAndTopicId(user.getId(), topic.getId())
                    .orElseGet(() -> TopicAffinity.builder()
                            .user(user)
                            .topic(topic)
                            .score(0)
                            .explicit(false)
                            .build());

            if (affinity.isExplicit()) {
                continue;
            }

            affinity.setScore(affinity.getScore() + reactionWeight);
            affinityRepository.save(affinity);
        }
    }

    @Transactional
    public List<TopicResponse> setExplicitTopics(User user, List<Long> topicIds) {
        Set<Long> wanted = topicIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(topicIds);

        List<TopicAffinity> existing = affinityRepository.findByUserIdOrderByScoreDesc(user.getId());
        for (TopicAffinity affinity : existing) {
            if (affinity.isExplicit() && !wanted.contains(affinity.getTopic().getId())) {
                affinity.setExplicit(false);
                affinityRepository.save(affinity);
            }
        }

        for (Topic topic : topicRepository.findAllById(wanted)) {
            TopicAffinity affinity = affinityRepository
                    .findByUserIdAndTopicId(user.getId(), topic.getId())
                    .orElseGet(() -> TopicAffinity.builder()
                            .user(user)
                            .topic(topic)
                            .score(0)
                            .explicit(false)
                            .build());

            affinity.setExplicit(true);
            affinity.setScore(Math.max(affinity.getScore(), explicitScore));
            affinityRepository.save(affinity);
        }

        // a selection was made -> onboarding is complete
        user.setOnboarded(true);
        userRepository.save(user);

        return affinitiesOf(user);
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> affinitiesOf(User user) {
        return affinityRepository.findByUserIdOrderByScoreDesc(user.getId()).stream()
                .map(affinity -> TopicResponse.builder()
                        .id(affinity.getTopic().getId())
                        .slug(affinity.getTopic().getSlug())
                        .label(affinity.getTopic().getLabel())
                        .category(affinity.getTopic().getCategory().name())
                        .affinity(round(affinity.getScore()))
                        .explicit(affinity.isExplicit())
                        .build())
                .toList();
    }

    /** Which topics count as "liked" (weight above the threshold). */
    @Transactional(readOnly = true)
    public Set<Long> likedTopicIds(Long userId) {
        Set<Long> liked = new LinkedHashSet<>();

        for (TopicAffinity affinity : affinityRepository.findByUserIdOrderByScoreDesc(userId)) {
            if (affinity.isExplicit() || affinity.getScore() >= threshold) {
                liked.add(affinity.getTopic().getId());
            }
        }
        return liked;
    }

    /** Category-level weight used when there is no topic. */
    @Transactional(readOnly = true)
    public Map<Category, Double> categoryAffinity(Long userId) {
        Map<Category, Double> map = new HashMap<>();

        for (TopicAffinity affinity : affinityRepository.findByUserIdOrderByScoreDesc(userId)) {
            Category category = affinity.getTopic().getCategory();
            map.merge(category, affinity.getScore(), Double::sum);
        }
        return map;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}