package com.kisansetu.user.dto;

import com.kisansetu.security.Role;
import com.kisansetu.user.entity.Profile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String fullName,
        String phone,
        String address,
        String city,
        String state,
        String pincode,
        String avatarUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        List<Role> roles
) {

    public static ProfileResponse from(Profile profile, List<Role> roles) {
        return new ProfileResponse(
                profile.getUserId(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getAddress(),
                profile.getCity(),
                profile.getState(),
                profile.getPincode(),
                profile.getAvatarUrl(),
                profile.getLatitude(),
                profile.getLongitude(),
                roles
        );
    }

    public static ProfileResponse publicFrom(Profile profile) {
        return new ProfileResponse(
                profile.getUserId(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getAddress(),
                profile.getCity(),
                profile.getState(),
                profile.getPincode(),
                profile.getAvatarUrl(),
                profile.getLatitude(),
                profile.getLongitude(),
                null
        );
    }
}