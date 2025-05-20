package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.admin;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.PasswordUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserStatusUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.debug("GET /api/admin/users");
        List<UserResponse> users = userService.findAllUsers().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
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
    public ResponseEntity<?> updatePassword(@PathVariable Long id,
                                            @Valid @RequestBody PasswordUpdateRequest req) {
        log.debug("PUT /api/admin/users/{}/password", id);
        try {
            userService.updatePassword(id, req.getNewPassword());
            return ResponseEntity.ok("Contraseña actualizada exitosamente.");
        } catch (ResourceNotFoundException e) {
            log.warn("Usuario no encontrado para actualizar contraseña con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
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
}
