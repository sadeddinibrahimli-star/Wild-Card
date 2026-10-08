package com.wildcard.common;

import com.wildcard.common.enums.AccountStatus;
import com.wildcard.users.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new BusinessException("Authentication required");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static void requireCanPublish(User user) {
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new BusinessException("Your account is suspended");
        }
        if (user.getAccountStatus() == AccountStatus.RESTRICTED) {
            throw new BusinessException("Your account is restricted from posting");
        }
    }
}