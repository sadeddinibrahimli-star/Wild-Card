package com.wildcard.users;

import com.wildcard.common.enums.AccountStatus;
import com.wildcard.users.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    List<User> findByRole(Role role);
    List<User> findByAccountStatus(AccountStatus accountStatus);
    long countByAccountStatus(AccountStatus accountStatus);

    /** Son 24 saatda aktiv olmus istifadeci sayi (hesab statusu deyil, REAL aktivlik). */
    long countByLastActiveAtAfter(java.time.Instant since);

    /** Admin panel: son 24 saatda aktif + hesab statusu ile birlikde. */
    @org.springframework.data.jpa.repository.Query("""
            select count(u) from User u
            where u.lastActiveAt >= :since
              and u.accountStatus = com.wildcard.common.enums.AccountStatus.ACTIVE
            """)
    long countActiveInLast24Hours(@org.springframework.data.repository.query.Param("since") java.time.Instant since);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Admin axtarisi - filtr bazada olur (Java-da süzəndə səhifələmə
     * problemi yaranır: 20-ci səhifədəki istifadəçi tapılmırdı).
     */
    @org.springframework.data.jpa.repository.Query("""
            select u from User u
            where (:q = '' or lower(u.username) like lower(concat('%', :q, '%'))
                            or lower(u.email) like lower(concat('%', :q, '%')))
              and (:status is null or u.accountStatus = :status)
              and (:role is null or u.role = :role)
            order by u.createdAt desc, u.id desc
            """)
    Page<User> adminSearch(@org.springframework.data.repository.query.Param("q") String q,
                           @org.springframework.data.repository.query.Param("status")
                           com.wildcard.common.enums.AccountStatus status,
                           @org.springframework.data.repository.query.Param("role")
                           com.wildcard.users.enums.Role role,
                           Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCaseOrderByCreatedAtDesc(String username, Pageable pageable);

    /**
     * Discover: aktiv istifadəcilər, cavanlar əvvəl.
     */
    @org.springframework.data.jpa.repository.Query("""
            select u from User u
            where u.id <> :excludeId
              and u.accountStatus = com.wildcard.common.enums.AccountStatus.ACTIVE
              and (:followingIds is null or u.id not in :followingIds)
            order by u.lastActiveAt desc nulls last, u.currentStreak desc, u.totalXp desc
            """)
    Page<User> findDiscoverCandidates(@org.springframework.data.repository.query.Param("excludeId") Long excludeId,
                                      @org.springframework.data.repository.query.Param("followingIds") java.util.List<Long> followingIds,
                                      Pageable pageable);
}
