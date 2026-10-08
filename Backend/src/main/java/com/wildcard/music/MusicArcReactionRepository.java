package com.wildcard.music;

import com.wildcard.reactions.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MusicArcReactionRepository extends JpaRepository<MusicArcReaction, Long> {

    Optional<MusicArcReaction> findByArcIdAndUserId(Long arcId, Long userId);

    List<MusicArcReaction> findByArcId(Long arcId);

    void deleteByArcId(Long arcId);
}
