package com.wildcard.social;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Query("select f.following.id from Follow f where f.follower.id = :followerId")
    List<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);

    @Query("select f from Follow f where f.follower.id = :followerId")
    Page<Follow> findAllByFollowerId(@Param("followerId") Long followerId, Pageable pageable);

    @Query("select f from Follow f where f.following.id = :followingId")
    Page<Follow> findAllByFollowingId(@Param("followingId") Long followingId, Pageable pageable);

    long countByFollowingId(Long followingId);
}
