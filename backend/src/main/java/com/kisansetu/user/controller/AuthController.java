package com.kisansetu.user.controller;

import com.kisansetu.common.ApiResponse;
import com.kisansetu.security.AuthUser;
import com.kisansetu.security.CurrentUser;
import com.kisansetu.user.dto.ProfileResponse;
import com.kisansetu.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Auth/session endpoints. Login, registration, password reset, OTP and
 * Google OAuth are handled entirely by Supabase Auth from the frontend;
 * this controller only reports the authenticated identity so the
 * frontend can bootstrap the app.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication identity endpoints (Supabase Auth owns credentials)")
public class AuthController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user identity and roles")
    public ApiResponse<Map<String, Object>> me() {
        AuthUser user = CurrentUser.get();
        ProfileResponse profile = profileService.getMyProfile(user);
        return ApiResponse.ok(Map.of(
                "id", profile.userId().toString(),
                "email", user.email(),
                "roles", profile.roles().stream().map(Enum::name).map(String::toLowerCase).toList(),
                "profile", profile
        ));
    }
}