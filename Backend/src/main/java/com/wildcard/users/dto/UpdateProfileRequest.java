package com.wildcard.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Öz profilinin redaktəsi (PUT /users/me).
 * Hər sahə OPTIONSALDIR: null gələn sahə dəyişmir.
 * Parol dəyişmək üçün currentPassword məcburidir (yoxlanışı servisdədir).
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

    /** Yeni istifadəçi adı (3-50 simvol, unikal). */
    @Size(min = 3, max = 50)
    private String username;

    /** Yeni e-poçt (unikal). */
    @Email
    @Size(max = 255)
    private String email;

    /** Parolu dəyişmək üçün cari parol — newPassword varsa məcburidir. */
    @Size(max = 100)
    private String currentPassword;

    /** Yeni parol: min 8 simvol, ən azı 1 hərf + 1 rəqəm (reset-flow ilə eyni qayda). */
    @Size(min = 8, max = 100)
    @Pattern(regexp = ".*[A-Za-z].*", message = "password must contain a letter")
    @Pattern(regexp = ".*[0-9].*", message = "password must contain a digit")
    private String newPassword;
}
