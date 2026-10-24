package com.kisansetu.user.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.security.AuthUser;
import com.kisansetu.security.Role;
import com.kisansetu.user.dto.ProfileRequest;
import com.kisansetu.user.dto.ProfileResponse;
import com.kisansetu.user.entity.Profile;
import com.kisansetu.user.repository.ProfileRepository;
import com.kisansetu.user.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final UUID USER = UUID.fromString("a0000000-0000-4000-8000-000000000021");

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private UserRoleRepository userRoleRepository;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(profileRepository, userRoleRepository);
    }

    @Test
    void getMyProfile_autoCreatesMissingProfile() {
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of(Role.CUSTOMER));

        ProfileResponse response = service.getMyProfile(new AuthUser(USER, "ravi@demo.in", null));

        assertEquals("ravi", response.fullName());
        assertTrue(response.roles().contains(Role.CUSTOMER));
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void getMyProfile_returnsExisting() {
        Profile profile = new Profile();
        profile.setUserId(USER);
        profile.setFullName("Ravi Kumar");
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.of(profile));
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of(Role.CUSTOMER));

        ProfileResponse response = service.getMyProfile(new AuthUser(USER, "ravi@demo.in", null));

        assertEquals("Ravi Kumar", response.fullName());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updateMyProfile_updatesFields() {
        Profile profile = new Profile();
        profile.setUserId(USER);
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of(Role.CUSTOMER));

        ProfileResponse response = service.updateMyProfile(new AuthUser(USER, "ravi@demo.in", null),
                new ProfileRequest("Ravi Kumar", "9876543210", "MG Road", "Kolhapur", "Maharashtra",
                        "416001", null, null, null));

        assertEquals("Ravi Kumar", response.fullName());
        assertEquals("9876543210", response.phone());
        verify(profileRepository).save(profile);
    }

    @Test
    void updateMyProfile_missingProfileNotFound() {
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateMyProfile(new AuthUser(USER, "x@y.in", null),
                        new ProfileRequest("A", null, null, null, null, null, null, null, null)));
        assertEquals(404, ex.getStatus());
    }

    @Test
    void getProfileEntityByUserId_notFoundForUnknown() {
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> service.getProfileEntityByUserId(USER));
    }

    @Test
    void hasRole_delegatesToRepository() {
        when(userRoleRepository.existsByUserIdAndRole(USER, Role.LOGISTICS)).thenReturn(true);
        assertTrue(service.hasRole(USER, Role.LOGISTICS));
    }
}
