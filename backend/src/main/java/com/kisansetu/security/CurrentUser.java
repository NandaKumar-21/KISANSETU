package com.kisansetu.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * Resolves the authenticated user. Throws 401 when absent.
     */
    public static AuthUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new com.kisansetu.common.exception.ApiException(401, "Authentication required");
        }
        return authUser;
    }

    public static UUID id() {
        return get().userId();
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthUser;
    }
}