package com.wildcard.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Editing your own profile (PUT /users/me).
 * Every field is OPTIONAL: a null field is not changed.
 * Changing the password requires currentPassword (validated in the service).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    @Size(max = 255)
    private String avatarUrl;

    @Size(max = 500)
    private String bio;

    @Size(min = 3, max = 50)
    private String username;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 100)
    private String currentPassword;

    /** New password: min 8 chars, at least one letter and one digit (same rule as the reset flow). */
    @Size(min = 8, max = 100)
    @Pattern(regexp = ".*[A-Za-z].*", message = "password must contain a letter")
    @Pattern(regexp = ".*[0-9].*", message = "password must contain a digit")
    private String newPassword;
}
