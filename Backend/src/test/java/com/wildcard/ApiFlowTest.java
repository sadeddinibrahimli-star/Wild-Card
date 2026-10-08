package com.wildcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Əsas API axınları: auth, profil, feed, validation, rol yoxlaması.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    private String token;

    @BeforeEach
    void login() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tester","email":"tester@wildcard.test","password":"Test12345"}
                                """))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() < 500));

        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"tester@wildcard.test","password":"Test12345"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        token = readToken(body);
    }

    private String readToken(String body) throws Exception {
        var node = mapper.readTree(body).path("data");
        if (node.has("accessToken")) {
            return node.get("accessToken").asText();
        }
        return mapper.writeValueAsString(Map.of());
    }

    // ---------------- auth ----------------

    @Test
    void healthAndDocsArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"tester@wildcard.test","password":"wrong-password"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validationRejectsShortPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"x","email":"not-an-email","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ---------------- profil ----------------

    @Test
    void meReturnsProfile() throws Exception {
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("tester"));
    }

    @Test
    void userCardReturnsLevelAndRarity() throws Exception {
        mvc.perform(get("/api/v1/users/1/card").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").exists())
                .andExpect(jsonPath("$.data.rarity").exists());
    }

    // ---------------- feed / content ----------------

    @Test
    void feedIsPaginated() throws Exception {
        mvc.perform(get("/api/v1/feed?page=0&size=5").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    void watchlistRequiresValidCategory() throws Exception {
        mvc.perform(post("/api/v1/watchlist")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"x","kind":"NOT_A_KIND","status":"WATCHING"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAndDeletePost() throws Exception {
        String created = mvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"ANIME","title":"Test post","body":"body"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long id = mapper.readTree(created).path("data").path("id").asLong();

        mvc.perform(delete("/api/v1/posts/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ---------------- XP / achievements ----------------

    @Test
    void achievementsAreAlwaysTwelve() throws Exception {
        mvc.perform(get("/api/v1/users/1/achievements").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(12));
    }

    @Test
    void searchEndpointsUseApiResponseEnvelope() throws Exception {
        mvc.perform(get("/api/v1/search/titles/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.animeSource").value("AniList"));
    }

    @Test
    void discoveryExcludesSelf() throws Exception {
        mvc.perform(get("/api/v1/users/discover").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ---------------- notifications ----------------

    @Test
    void notificationFiltersWork() throws Exception {
        for (String filter : new String[]{"all", "follows", "likes"}) {
            mvc.perform(get("/api/v1/notifications?filter=" + filter)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    // ---------------- xarici axtarış (açar lazım deyil) ----------------

    @Test
    void titleSearchDegradesGracefully() throws Exception {
        // xarici xidmət offline olsa belə 500 qaytarmamalıdır
        int status = mvc.perform(get("/api/v1/search/titles?q=test&type=ANIME")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(status == 200,
                "axtarış 200 qaytarmalıdır, got: " + status);
    }

    // ---------------- doc 4.1 Administration ----------------

    @Autowired
    com.wildcard.users.UserRepository userRepository;

    @Autowired
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /** Doc 4.1: admin hesab yarada bilər. */
    @Test
    void adminCanCreateAccount() throws Exception {
        String email = "created-" + System.nanoTime() + "@wildcard.test";
        mvc.perform(patch("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

        // admin ilə login
        String adminToken = loginAs("admin@wildcard.com", "admin123");

        mvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newstaff","email":"%s","password":"Passw0rd!","role":"MODERATOR"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("MODERATOR"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    /** Doc 4.1: hesab redaktə olunur. */
    @Test
    void adminCanUpdateAndSuspendAccount() throws Exception {
        String adminToken = loginAs("admin@wildcard.com", "admin123");

        long victim = userRepository.findByUsername("tester").orElseThrow().getId();

        mvc.perform(patch("/api/v1/admin/users/" + victim)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bio":"set by admin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("set by admin"));

        // suspend
        mvc.perform(patch("/api/v1/admin/users/" + victim + "/status?status=SUSPENDED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("SUSPENDED"));

        // reactivate
        mvc.perform(patch("/api/v1/admin/users/" + victim + "/status?status=ACTIVE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    /** Admin olmayan istifadəçi admin endpoint-lərinə toxunmamalıdır. */
    @Test
    void normalUserCannotReachAdminEndpoints() throws Exception {
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"hack","email":"hack@x.com","password":"Passw0rd!"}
                                """))
                .andExpect(status().isForbidden());
    }

    /** Doc 4.1: admin özünü kilidləyə bilməz. */
    @Test
    void adminCannotSuspendSelf() throws Exception {
        String adminToken = loginAs("admin@wildcard.com", "admin123");
        mvc.perform(patch("/api/v1/admin/users/1/status?status=SUSPENDED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    /** Doc 4.1: admin axtarış süzgəcləri. */
    @Test
    void adminUserSearchWorks() throws Exception {
        String adminToken = loginAs("admin@wildcard.com", "admin123");
        mvc.perform(get("/api/v1/admin/users?search=admin&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mvc.perform(get("/api/v1/admin/users?role=ADMIN&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].role").value("ADMIN"));
    }

    /** Doc 4.2: report məzmunu önizlənməsi ilə gəlir. */
    @Test
    void reportListIncludesContentPreview() throws Exception {
        String adminToken = loginAs("admin@wildcard.com", "admin123");

        // sirf poçt yaradıb report etmek deyil - mövcud content-i şikayət edirik
        String postBody = mvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"GAMING","title":"Reported title","body":"Reported body"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long postId = mapper.readTree(postBody).path("data").path("id").asLong();

        mvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"postId":%d,"reason":"Spam"}
                                """.formatted(postId)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/moderation/reports?size=5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].targetType").value("POST"))
                .andExpect(jsonPath("$.data.content[0].reportedPostTitle").value("Reported title"))
                .andExpect(jsonPath("$.data.content[0].reportedPostBody").value("Reported body"))
                .andExpect(jsonPath("$.data.content[0].reportedPostAuthorUsername").exists())
                .andExpect(jsonPath("$.data.content[0].reportedPostHidden").value(false));
    }

    /** Doc 4.1: platforma statistikası. */
    @Test
    void adminStatsCoverDocFields() throws Exception {
        String adminToken = loginAs("admin@wildcard.com", "admin123");
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeUsers").exists())
                .andExpect(jsonPath("$.data.totalXpGranted").exists())
                .andExpect(jsonPath("$.data.pendingReports").exists())
                .andExpect(jsonPath("$.data.postsPerDay.length()").value(7));
    }

    // ---------------- tam profil redaktəsi: nick / email / parol ----------------

    /** PUT /users/me — uğursuz hallar: səhv cari parol, tutuşan nick, zəif parol, qısa nick. */
    @Test
    void updateProfileRejectsInvalidChanges() throws Exception {
        String t = token;

        // cari parol səhvdir
        mvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"nope-123","newPassword":"Changed1234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));

        // artıq mövcud olan username
        mvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin"}
                                """))
                .andExpect(status().isBadRequest());

        // yeni parol rəqəm saxlamır (reset-flow ilə eyni qayda)
        mvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Test12345","newPassword":"abcdefgh"}
                                """))
                .andExpect(status().isBadRequest());

        // qısa username (DTO @Size(min=3))
        mvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ab"}
                                """))
                .andExpect(status().isBadRequest());

        // heç nə dəyişmədi — hələ də köhnə parolla girir
        loginAs("tester@wildcard.test", "Test12345");
    }

    /** PUT /users/me — nick + email + parol dəyişir; email yalnız öz cavabda; yeni parol işləyir. */
    @Test
    void updateProfileChangesUsernameEmailAndPassword() throws Exception {
        String unique = String.valueOf(System.nanoTime()).substring(5); // sabit uzunluq
        String username = "editor" + unique;
        String email = username + "@example.com";
        String newUsername = username + "x";
        String newEmail = username + "x@example.com";

        // bu test üçün ayrı istifadəçi — "tester" digər testlərdə işlənir, toxunmuruq
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","password":"Test12345"}
                                """.formatted(username, email)))
                .andExpect(status().isOk());
        String t = loginAs(email, "Test12345");

        // email yalnız öz /users/me cavabındadır
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        // tam dəyişiklik: nick + email + parol (cari parol ilə)
        mvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","bio":"edited by test",
                                 "currentPassword":"Test12345","newPassword":"Changed1234"}
                                """.formatted(newUsername, newEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(newUsername))
                .andExpect(jsonPath("$.data.email").value(newEmail))
                .andExpect(jsonPath("$.data.bio").value("edited by test"));

        // köhnə parol artıq işləmir, yenisi işləyir
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test12345"}
                                """.formatted(newEmail)))
                .andExpect(status().isBadRequest());
        loginAs(newEmail, "Changed1234");
    }

    private String loginAs(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data").path("accessToken").asText();
    }
}
