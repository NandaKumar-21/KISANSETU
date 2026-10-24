package com.kisansetu.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated principal derived from a validated Supabase JWT.
 * Roles are loaded from the database (user_roles), never from the token,
 * so a role can never be forged by a client.
 */
public record AuthUser(UUID userId, String email, Collection<String> authorities) {

    public static AuthUser of(UUID userId, String email, List<Role> roles) {
        return new AuthUser(userId, email, roles.stream().map(Role::authority).toList());
    }

    public boolean hasRole(Role role) {
        return authorities.contains(role.authority());
    }
}