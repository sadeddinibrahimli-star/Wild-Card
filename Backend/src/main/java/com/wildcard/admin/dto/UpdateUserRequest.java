package com.wildcard.admin.dto;

import com.wildcard.common.enums.AccountStatus;
import com.wildcard.users.enums.Role;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin edits an account.
 * Every field is optional: a null field is not changed.
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

    @Size(min = 8, max = 100)
    private String password;

    private Boolean onboarded;
}