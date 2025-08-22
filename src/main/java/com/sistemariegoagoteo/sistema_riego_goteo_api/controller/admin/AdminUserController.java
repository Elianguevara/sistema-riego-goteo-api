package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.admin;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserStatusUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.AdminPasswordUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para que los Administradores gestionen usuarios.
 * Todos los endpoints requieren autenticación y rol ADMIN.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
// @PreAuthorize("hasRole('ADMIN')") // <- Eliminamos la seguridad a nivel de clase
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // <- Añadimos seguridad a nivel de método
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.debug("GET /api/admin/users");
        List<UserResponse> users = userService.findAllUsers().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // <- Añadimos seguridad a nivel de método
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.debug("GET /api/admin/users/{}", id);
        try {
            User user = userService.findUserById(id);
            return ResponseEntity.ok(new UserResponse(user));
        } catch (ResourceNotFoundException e) {
            log.warn("Usuario no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasRole('ADMIN')") // <- Añadimos seguridad a nivel de método
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String roleName) {
        log.debug("GET /api/admin/users/role/{}", roleName);
        try {
            List<UserResponse> users = userService.findUsersByRole(roleName).stream()
                    .map(UserResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (ResourceNotFoundException e) {
            log.warn("Rol no encontrado: {}", roleName);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // <- Añadimos seguridad a nivel de método
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @Valid @RequestBody UserUpdateRequest updateRequest) {
        log.debug("PUT /api/admin/users/{}", id);
        try {
            User updated = userService.updateUser(id, updateRequest);
            return ResponseEntity.ok(new UserResponse(updated));
        } catch (ResourceNotFoundException e) {
            log.warn("Usuario no encontrado para actualizar con ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            log.error("Error al actualizar usuario {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')") // <- Añadimos seguridad a nivel de método
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id,
                                              @Valid @RequestBody UserStatusUpdateRequest statusRequest) {
        log.debug("PUT /api/admin/users/{}/status", id);
        try {
            User updated = userService.updateUserStatus(id, statusRequest.getActive());
            return ResponseEntity.ok(new UserResponse(updated));
        } catch (ResourceNotFoundException e) {
            log.warn("Usuario no encontrado para actualizar estado con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')") // <- Añadimos seguridad a nivel de método
    // Usamos el nuevo DTO específico para el admin
    public ResponseEntity<?> updatePassword(@PathVariable Long id,
                                            @Valid @RequestBody AdminPasswordUpdateRequest req) {
        log.debug("PUT /api/admin/users/{}/password", id);
        try {
            // El servicio ya tenía un método específico para el admin que no pide la clave antigua
            userService.updatePassword(id, req.getNewPassword());
            return ResponseEntity.ok("Contraseña actualizada exitosamente.");
        } catch (ResourceNotFoundException e) {
            log.warn("Usuario no encontrado para actualizar contraseña con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // <- Añadimos seguridad a nivel de método
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.debug("DELETE /api/admin/users/{}", id);
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("Usuario no encontrado para eliminar con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Asigna un usuario específico a una finca específica.
     */
    @PostMapping("/{userId}/farms/{farmId}")
    @PreAuthorize("hasRole('ADMIN')") // <- Añadimos seguridad a nivel de método
    public ResponseEntity<?> assignUserToFarm(@PathVariable Long userId, @PathVariable Integer farmId) {
        log.info("Solicitud POST para asignar usuario ID {} a finca ID {}", userId, farmId);
        try {
            userService.assignUserToFarm(userId, farmId);
            return ResponseEntity.ok().body(
                    String.format("Usuario con ID %d asignado exitosamente a la finca con ID %d.", userId, farmId)
            );
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo asignar usuario a finca: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    /**
     * Desasigna un usuario específico de una finca específica.
     */
    @DeleteMapping("/{userId}/farms/{farmId}")
    @PreAuthorize("hasRole('ADMIN')") // <- Añadimos seguridad a nivel de método
    public ResponseEntity<?> unassignUserFromFarm(@PathVariable Long userId, @PathVariable Integer farmId) {
        log.info("Solicitud DELETE para desasignar usuario ID {} de la finca ID {}", userId, farmId);
        try {
            userService.unassignUserFromFarm(userId, farmId);
            return ResponseEntity.noContent().build(); // 204 No Content es apropiado para un DELETE exitoso
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo desasignar usuario de finca: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene una lista de todos los usuarios asignados a una finca específica.
     */
    @GetMapping("/farms/{farmId}/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA')")
    public ResponseEntity<?> getUsersAssignedToFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener usuarios de la finca ID {}", farmId);
        try {
            List<UserResponse> users = userService.findUsersByFarm(farmId).stream()
                    .map(UserResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudieron obtener usuarios, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

}