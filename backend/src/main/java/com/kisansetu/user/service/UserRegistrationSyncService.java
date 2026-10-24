package com.kisansetu.user.service;

import com.kisansetu.security.Role;
import com.kisansetu.user.entity.Profile;
import com.kisansetu.user.entity.UserRole;
import com.kisansetu.user.repository.ProfileRepository;
import com.kisansetu.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Synchronizes application records for an authenticated Supabase Auth user.
 * <p>
 * Supabase Auth users and the application's user tables are separate concepts.
 * A user created in Supabase Auth has no application profile or role until
 * this service provisions them. The canonical identity is the Supabase user
 * UUID (JWT {@code sub}), never the email address.
 * <p>
 * Provisioning is idempotent and non-destructive:
 * <ul>
 *   <li>a {@code profiles} row is created only if missing (name taken from
 *       {@code user_metadata.full_name}, else the email prefix);</li>
 *   <li>a {@code user_roles} row is created only when the user has NO roles at
 *       all and {@code user_metadata.role} is one of the four app roles;</li>
 *   <li>existing roles are never overwritten or duplicated.</li>
 * </ul>
 * A user without a role in the metadata stays role-less (unauthorized).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationSyncService {

    private final ProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public List<Role> synchronize(UUID userId, String email, String fullName, String metadataRole) {
        return synchronize(userId, email, fullName, metadataRole, null, null, null, null);
    }

    @Transactional
    public List<Role> synchronize(UUID userId, String email, String fullName, String metadataRole,
                                  String authProvider, String googleProviderId, String googleEmail, String avatarUrl) {
        Optional<Profile> existing = profileRepository.findByUserId(userId);
        boolean profileCreated = existing.isEmpty();
        existing.orElseGet(() -> {
            Profile created = new Profile();
            created.setUserId(userId);
            created.setFullName(profileName(fullName, email));
            created.setAuthProvider(authProvider != null ? authProvider : "EMAIL");
            if (googleProviderId != null) {
                created.setGoogleProviderId(googleProviderId);
            }
            if (googleEmail != null) {
                created.setGoogleEmail(googleEmail);
            }
            if (avatarUrl != null) {
                created.setAvatarUrl(avatarUrl);
            }
            return profileRepository.save(created);
        });

        // Update existing profile with Google info if newly provided
        if (existing.isPresent() && !profileCreated) {
            Profile profile = existing.get();
            boolean updated = false;
            if (authProvider != null && (profile.getAuthProvider() == null || profile.getAuthProvider().equals("EMAIL"))) {
                profile.setAuthProvider(authProvider);
                updated = true;
            }
            if (googleProviderId != null && profile.getGoogleProviderId() == null) {
                profile.setGoogleProviderId(googleProviderId);
                updated = true;
            }
            if (googleEmail != null && profile.getGoogleEmail() == null) {
                profile.setGoogleEmail(googleEmail);
                updated = true;
            }
            if (avatarUrl != null && profile.getAvatarUrl() == null) {
                profile.setAvatarUrl(avatarUrl);
                updated = true;
            }
            if (fullName != null && !fullName.isBlank() && (profile.getFullName() == null || profile.getFullName().isBlank() || profile.getFullName().startsWith("User"))) {
                profile.setFullName(fullName.trim());
                updated = true;
            }
            if (updated) {
                profileRepository.save(profile);
            }
        }

        List<Role> roles = userRoleRepository.findRolesByUserId(userId);
        log.info("Current roles for user {} ({}): {}", userId, email, roles);
        if (roles.isEmpty() && metadataRole != null) {
            Role role = Role.fromDbValue(metadataRole);
            if (role != null) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRole(role);
                userRoleRepository.save(userRole);
                roles = List.of(role);
                log.info("Provisioned {} role for Supabase user {} ({}) from metadata", role, userId, email);
            } else {
                log.warn("Supabase user {} ({}) has metadata role '{}' which is not a valid app "
                        + "role; access stays unauthorized", userId, email, metadataRole);
            }
        } else if (roles.isEmpty()) {
            log.warn("Supabase user {} ({}) has no roles and no metadata role provided; user will be unauthorized", userId, email);
        }
        log.info("Final roles for Supabase user {} ({}): profile {}, roles {}", userId, email,
                profileCreated ? "created" : "exists", roles);
        return roles;
    }

    /**
     * Find existing user by Google provider ID or email for account linking
     */
    public Optional<Profile> findByGoogleIdentity(String googleProviderId, String googleEmail) {
        if (googleProviderId != null) {
            return profileRepository.findByGoogleProviderId(googleProviderId);
        }
        if (googleEmail != null) {
            return profileRepository.findByGoogleEmail(googleEmail);
        }
        return Optional.empty();
    }

    private String profileName(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }
        return "User";
    }
}