package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Permission.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByPermissionName(String permissionName);
}