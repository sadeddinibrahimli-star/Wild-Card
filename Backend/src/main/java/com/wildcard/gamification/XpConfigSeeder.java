package com.wildcard.gamification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fills the xp_config price table at startup.
 *
 * Nothing seeded it before: a fresh database kept xp_config empty and
 * changing a value from the admin panel returned "No XP config for POST"
 * (404). Doc 4.1: "Manage XP-value configuration".
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class XpConfigSeeder {

    private final XpService xpService;

    @Bean
    CommandLineRunner seedXpConfig() {
        return args -> {
            xpService.seedConfigIfEmpty();
            log.info("XP config ready ({} actions)", XpAction.values().length);
        };
    }
}
