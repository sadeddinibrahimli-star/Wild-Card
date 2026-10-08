package com.wildcard.gamification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface XpConfigRepository extends JpaRepository<XpConfig, Long> {

    Optional<XpConfig> findByAction(XpAction action);
}