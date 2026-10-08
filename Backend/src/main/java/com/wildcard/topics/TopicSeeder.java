package com.wildcard.topics;

import com.wildcard.posts.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Movzu siyahisini bir defe doldurur.
 * Burada AI yoxdur - movzular ellə teyin olunub, muellif post yazanda
 * ozeli sechir (autocomplete).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TopicSeeder {

    private final TopicRepository topicRepository;

    private static final List<Topic> DEFAULTS = List.of(
            new Topic(null, "pokemon", "Pokemon", Category.GAMING),
            new Topic(null, "zelda", "Zelda", Category.GAMING),
            new Topic(null, "elden-ring", "Elden Ring", Category.GAMING),
            new Topic(null, "naruto", "Naruto", Category.ANIME),
            new Topic(null, "one-piece", "One Piece", Category.ANIME),
            new Topic(null, "frieren", "Frieren", Category.ANIME),
            new Topic(null, "jojo", "JoJo", Category.ANIME),
            new Topic(null, "demon-slayer", "Demon Slayer", Category.ANIME),
            new Topic(null, "marvel", "Marvel", Category.FILM),
            new Topic(null, "dc-comics", "DC Comics", Category.FILM),
            new Topic(null, "horror", "Horror", Category.FILM),
            new Topic(null, "studio-ghibli", "Studio Ghibli", Category.ANIME),
            new Topic(null, "yoasobi", "Yoasobi", Category.MUSIC),
            new Topic(null, "j-rock", "J-Rock", Category.MUSIC),
            new Topic(null, "bump-of-chicken", "Bump of Chicken", Category.MUSIC),
            new Topic(null, "kpop", "K-Pop", Category.MUSIC),
            new Topic(null, "lofi", "Lo-fi", Category.MUSIC),
            new Topic(null, "dnd-5e", "D&D 5e", Category.DND),
            new Topic(null, "pathfinder", "Pathfinder", Category.DND),
            new Topic(null, "critical-role", "Critical Role", Category.DND)
    );

    @Bean
    CommandLineRunner seedTopics() {
        return args -> {
            if (topicRepository.count() > 0) {
                return;
            }

            for (Topic topic : DEFAULTS) {
                if (topicRepository.findBySlug(topic.getSlug()).isEmpty()) {
                    topicRepository.save(topic);
                }
            }

            log.info("Seeded {} topics", topicRepository.count());
        };
    }
}