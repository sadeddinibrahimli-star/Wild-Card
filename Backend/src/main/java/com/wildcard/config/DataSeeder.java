package com.wildcard.config;

import com.wildcard.common.enums.AccountStatus;
import com.wildcard.users.User;
import com.wildcard.users.UserRepository;
import com.wildcard.users.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * İlk admin hesabının yaradılması.
 *
 * QAYDALAR (təhlükəsizlik):
 *  - parol HEÇ VAXT kodda yazılmır; yalnız env-dən gəlir
 *  - parol boş və ya çox qısa olanda dayanır (tətbiq işləməz)
 *  - eyni e-poçt artıq varsa toxunmur (təkrar seed etmir)
 *  - parol loga YAZILMIR, e-poçt yazılır
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WildcardProperties props;

    @Bean
    CommandLineRunner seedAdmin() {
        return args -> {
            var cfg = props.getAdmin();

            if (!cfg.isEnabled()) {
                log.info("Admin seeding disabled (wildcard.admin.enabled=false)");
                return;
            }

            String email = cfg.getEmail() == null ? "" : cfg.getEmail().trim();
            String password = cfg.getPassword() == null ? "" : cfg.getPassword();

            if (email.isEmpty()) {
                throw new IllegalStateException(
                        "wildcard.admin.enabled=true but ADMIN_EMAIL is empty. "
                                + "Set ADMIN_EMAIL in the environment or set wildcard.admin.enabled=false.");
            }
            if (password.length() < MIN_PASSWORD_LENGTH) {
                // parolun özü qeydə alınmır - yalnız uzunluq problemi bildirilir
                throw new IllegalStateException(
                        "ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH
                                + " characters (got " + password.length() + "). "
                                + "Set a real password in the environment.");
            }

            if (userRepository.existsByEmail(email)) {
                log.info("Admin account already exists, seeding skipped");
                return;
            }

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setAccountStatus(AccountStatus.ACTIVE);
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);

            log.info("Seeded admin account: {}", admin.getEmail());
        };
    }
}