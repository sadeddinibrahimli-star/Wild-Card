package com.wildcard.topics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicAffinityRepository extends JpaRepository<TopicAffinity, Long> {

    Optional<TopicAffinity> findByUserIdAndTopicId(Long userId, Long topicId);

    List<TopicAffinity> findByUserIdOrderByScoreDesc(Long userId);

    void deleteByUserIdAndTopicId(Long userId, Long topicId);
}
