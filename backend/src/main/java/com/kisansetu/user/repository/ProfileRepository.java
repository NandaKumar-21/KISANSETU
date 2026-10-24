package com.kisansetu.user.repository;

import com.kisansetu.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("select p from Profile p where p.userId in :userIds")
    List<Profile> findByUserIds(@Param("userIds") List<UUID> userIds);

    Optional<Profile> findByGoogleProviderId(String googleProviderId);

    Optional<Profile> findByGoogleEmail(String googleEmail);
}