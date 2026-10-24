package com.kisansetu.user.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.security.AuthUser;
import com.kisansetu.security.Role;
import com.kisansetu.user.dto.ProfileRequest;
import com.kisansetu.user.dto.ProfileResponse;
import com.kisansetu.user.entity.Profile;
import com.kisansetu.user.repository.ProfileRepository;
import com.kisansetu.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(AuthUser user) {
        Profile profile = profileRepository.findByUserId(user.userId())
                .orElseGet(() -> {
                    Profile created = new Profile();
                    created.setUserId(user.userId());
                    created.setFullName(user.email() != null ? user.email().split("@")[0] : "User");
                    return profileRepository.save(created);
                });
        List<Role> roles = userRoleRepository.findRolesByUserId(user.userId());
        return ProfileResponse.from(profile, roles);
    }

    @Transactional
    public ProfileResponse updateMyProfile(AuthUser user, ProfileRequest request) {
        Profile profile = profileRepository.findByUserId(user.userId())
                .orElseThrow(() -> ApiException.notFound("Profile not found. Please try again later."));
        profile.setFullName(request.fullName());
        profile.setPhone(request.phone());
        profile.setAddress(request.address());
        profile.setCity(request.city());
        profile.setState(request.state());
        profile.setPincode(request.pincode());
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.latitude() != null) {
            profile.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            profile.setLongitude(request.longitude());
        }
        profileRepository.save(profile);
        return ProfileResponse.from(profile, userRoleRepository.findRolesByUserId(user.userId()));
    }

    @Transactional(readOnly = true)
    public Profile getProfileEntityByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Profile not found"));
    }

    @Transactional(readOnly = true)
    public List<Profile> getProfilesByIds(List<UUID> userIds) {
        return profileRepository.findByUserIds(userIds);
    }

    @Transactional(readOnly = true)
    public boolean hasRole(UUID userId, Role role) {
        return userRoleRepository.existsByUserIdAndRole(userId, role);
    }
}