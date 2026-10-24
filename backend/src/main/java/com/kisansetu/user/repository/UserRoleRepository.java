package com.kisansetu.user.repository;

import com.kisansetu.security.Role;
import com.kisansetu.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserId(UUID userId);

    @Query("select ur.role from UserRole ur where ur.userId = :userId")
    List<Role> findRolesByUserId(@Param("userId") UUID userId);

    @Query("select ur.role from UserRole ur where ur.userId = :userId")
    List<String> findRoleStringsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndRole(UUID userId, Role role);

    @Query("select count(ur) > 0 from UserRole ur where ur.userId = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);
}