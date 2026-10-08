package com.wildcard.gamification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XP qiymətləri cədvəlini (xp_config) başlanışda doldurur.
 *
 * Əvvəl heç kim seed etmirdi: təzə bazada xp_config boş qalırdı və
 * admin panelindən qiymət dəyişmək "No XP config for POST" (404) verirdi.
 * Doc 4.1: "Manage XP-value configuration".
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
