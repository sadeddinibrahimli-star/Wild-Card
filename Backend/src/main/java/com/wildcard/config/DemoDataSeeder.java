package com.wildcard.config;

import com.wildcard.common.enums.AccountStatus;
import com.wildcard.music.MusicArc;
import com.wildcard.music.MusicArcRepository;
import com.wildcard.posts.Category;
import com.wildcard.posts.Post;
import com.wildcard.posts.PostRepository;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import com.wildcard.users.enums.Role;
import com.wildcard.watchlist.MediaKind;
import com.wildcard.watchlist.WatchStatus;
import com.wildcard.watchlist.WatchlistItem;
import com.wildcard.watchlist.WatchlistRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lokal demo məlumatı.
 *
 * YALNIZ SEED_ENABLED=true və boş bazada işləyir.
 * Heç bir parol koda yazılmır - ENV-dən gəlir.
 * İstehsal mühitində söndürülmüş qalır.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "wildcard.seed.enabled", havingValue = "true")
public class DemoDataSeeder {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final WatchlistRepository watchlistRepository;
    private final MusicArcRepository musicArcRepository;
    private final PasswordEncoder passwordEncoder;
    private final WildcardProperties props;

    public DemoDataSeeder(UserRepository userRepository,
                          PostRepository postRepository,
                          WatchlistRepository watchlistRepository,
                          MusicArcRepository musicArcRepository,
                          PasswordEncoder passwordEncoder,
                          WildcardProperties props) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.watchlistRepository = watchlistRepository;
        this.musicArcRepository = musicArcRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @org.springframework.context.annotation.Bean
    public CommandLineRunner seedDemoData() {
        return args -> {
            if (userRepository.count() > 1) {
                log.info("Demo seed skipped - database already has data");
                return;
            }
            String password = props.getSeed().getUserPassword();
            if (password == null || password.isBlank()) {
                log.info("Demo seed skipped - wildcard.seed.user-password is empty");
                return;
            }
            seed(password);
        };
    }

    @Transactional
    private void seed(String password) {
        List<User> users = List.of(
                user("kaori", "kaori@wildcard.local", "Gaming, J-Rock and long walks.", Role.USER),
                user("axel", "axel@wildcard.local", "Film only. I write essays about endings.", Role.USER),
                user("nina", "nina@wildcard.local", "Pokemon cartographer.", Role.USER),
                user("yusuf", "yusuf@wildcard.local", "DnD forever.", Role.USER));

        users = userRepository.saveAll(users);

        Post p1 = postRepository.save(post(users.get(0), Category.GAMING,
                "Finally finished the whole Elden Ring run",
                "No spoilers. I was stuck on the same boss for three days and the answer was patience."));

        Post p2 = postRepository.save(post(users.get(1), Category.FILM,
                "Best animated ending I've seen this year",
                "The last five minutes did more than the whole trilogy."));

        Post p3 = postRepository.save(post(users.get(2), Category.GAMING,
                "Built a living dex tracker",
                "Spent way too long on it. Turns out my living dex is 40% and my regret is 100%."));

        Post p4 = postRepository.save(post(users.get(3), Category.DND,
                "Our session turned into a horror game by accident",
                "One player rolled a cursed lantern and we rewrote the campaign in ten minutes."));

        p1.setHiddenByModeration(false);
        p2.setHiddenByModeration(false);
        postRepository.saveAll(List.of(p1, p2, p3, p4));

        watchlistRepository.saveAll(List.of(
                watchItem(users.get(0), "Frieren: Beyond Journey's End", MediaKind.ANIME, WatchStatus.WATCHING),
                watchItem(users.get(0), "Bocchi the Rock!", MediaKind.ANIME, WatchStatus.COMPLETED),
                watchItem(users.get(1), "Perfect Days", MediaKind.FILM, WatchStatus.PLAN_TO_WATCH),
                watchItem(users.get(2), "Pokemon Scarlet", MediaKind.FILM, WatchStatus.WATCHING),
                watchItem(users.get(3), "Baldur's Gate 3", MediaKind.FILM, WatchStatus.COMPLETED)));

        musicArcRepository.saveAll(List.of(
                musicArc(users.get(0), "YOASOBI", "Idol", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/3a/17/e/300x300bb.jpg"),
                musicArc(users.get(1), "Radiohead", "Weird Fishes / Arpeggi", null),
                musicArc(users.get(2), "Nujabes", "Aruarian Dance", null)));

        log.info("Seeded demo data: {} users, {} posts", users.size(), postRepository.count());
    }

    private User user(String username, String email, String bio, Role role) {
        return User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(props.getSeed().getUserPassword()))
                .bio(bio)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .onboarded(true)
                .currentStreak(0)
                .longestStreak(0)
                .totalXp(0)
                .build();
    }

    private Post post(User author, Category category, String title, String body) {
        return Post.builder()
                .author(author)
                .category(category)
                .title(title)
                .body(body)
                .build();
    }

    private WatchlistItem watchItem(User user, String title, MediaKind kind, WatchStatus status) {
        return WatchlistItem.builder()
                .user(user)
                .title(title)
                .kind(kind)
                .status(status)
                .build();
    }

    private MusicArc musicArc(User user, String artist, String track, String albumArtUrl) {
        return MusicArc.builder()
                .user(user)
                .artist(artist)
                .trackName(track)
                .albumArtUrl(albumArtUrl)
                .current(true)
                .startedAt(LocalDateTime.now().minusDays(3))
                .build();
    }
}