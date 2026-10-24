package com.kisansetu.user.controller;

import com.kisansetu.common.ApiResponse;
import com.kisansetu.security.AuthUser;
import com.kisansetu.security.CurrentUser;
import com.kisansetu.security.Role;
import com.kisansetu.user.dto.ProfileRequest;
import com.kisansetu.user.dto.ProfileResponse;
import com.kisansetu.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "User profile management")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile and roles")
    public ApiResponse<ProfileResponse> me() {
        return ApiResponse.ok(profileService.getMyProfile(CurrentUser.get()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's profile")
    public ApiResponse<ProfileResponse> update(@Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok(profileService.updateMyProfile(CurrentUser.get(), request));
    }

    @GetMapping("/roles")
    @Operation(summary = "List the authenticated user's roles")
    public ApiResponse<List<Role>> myRoles() {
        return ApiResponse.ok(profileService.getMyProfile(CurrentUser.get()).roles());
    }
}