package com.kisansetu.user.service;

import com.kisansetu.security.Role;
import com.kisansetu.user.entity.Profile;
import com.kisansetu.user.entity.UserRole;
import com.kisansetu.user.repository.ProfileRepository;
import com.kisansetu.user.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationSyncServiceTest {

    private static final UUID USER = UUID.fromString("a96d8a6d-adfe-4ecb-948f-c575a1262fca");

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private UserRoleRepository userRoleRepository;

    private UserRegistrationSyncService service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationSyncService(profileRepository, userRoleRepository);
    }

    @Test
    void synchronize_createsProfileAndRoleFromMetadata() {
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of());

        List<Role> roles = service.synchronize(USER, "farmer.ramesh@kisansetu.demo",
                "Ramesh Patil", "farmer");

        assertEquals(List.of(Role.FARMER), roles);
        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertEquals(USER, profileCaptor.getValue().getUserId());
        assertEquals("Ramesh Patil", profileCaptor.getValue().getFullName());
        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        assertEquals(USER, roleCaptor.getValue().getUserId());
        assertEquals(Role.FARMER, roleCaptor.getValue().getRole());
    }

    @Test
    void synchronize_fullNameFallsBackToEmailPrefix() {
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of());

        service.synchronize(USER, "farmer.ramesh@kisansetu.demo", null, null);

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals("farmer.ramesh", captor.getValue().getFullName());
    }

    @Test
    void synchronize_noMetadataRole_leavesUserUnauthorized() {
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of());

        List<Role> roles = service.synchronize(USER, "farmer.ramesh@kisansetu.demo", null, null);

        assertTrue(roles.isEmpty());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void synchronize_invalidMetadataRole_isIgnored() {
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of());

        List<Role> roles = service.synchronize(USER, "farmer.ramesh@kisansetu.demo", null, "admin");

        assertTrue(roles.isEmpty());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void synchronize_existingProfileIsNotDuplicated() {
        Profile profile = new Profile();
        profile.setUserId(USER);
        profile.setFullName("Ramesh Patil");
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.of(profile));
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of());

        List<Role> roles = service.synchronize(USER, "farmer.ramesh@kisansetu.demo", null, "farmer");

        assertEquals(List.of(Role.FARMER), roles);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void synchronize_existingRolesArePreserved() {
        Profile profile = new Profile();
        profile.setUserId(USER);
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.of(profile));
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of(Role.MERCHANT));

        List<Role> roles = service.synchronize(USER, "m@kisansetu.demo", null, "farmer");

        assertEquals(List.of(Role.MERCHANT), roles);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void synchronize_createsOnlyProfileWhenNoRoleGiven() {
        when(profileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRoleRepository.findRolesByUserId(USER)).thenReturn(List.of());

        service.synchronize(USER, "x@y.in", "Some Name", null);

        verify(profileRepository).save(any(Profile.class));
        verify(userRoleRepository, never()).save(any());
    }
}