package com.wildcard.admin.dto;

import com.wildcard.common.enums.AccountStatus;
import com.wildcard.users.enums.Role;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin tərəfindən hesabın redaktəsi (doc 4.1: "update ... user accounts").
 * Null olan sahə DƏYİŞMİR.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(max = 30)
    private String username;

    @Size(max = 120)
    private String email;

    @Size(max = 500)
    private String bio;

    @Size(max = 500)
    private String avatarUrl;

    private Role role;

    private AccountStatus accountStatus;

    /** Yeni parol təyin etmək üçün (boşdursa dəyişmir). */
    @Size(min = 8, max = 100)
    private String password;

    /** Onboarding vəziyyətini sıfırlamaq. */
    private Boolean onboarded;
}