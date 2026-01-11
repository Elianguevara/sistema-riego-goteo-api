package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.admin;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.AdminPasswordUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserStatusUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class AdminUserController {

    private final UserService userService;

    /**
     * Obtiene un listado paginado de todos los usuarios del sistema.
     *
     * @param pageable Configuración de paginación.
     * @return Página de usuarios.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        log.debug("GET /api/admin/users");
        Page<UserResponse> usersResponse = userService.findAllUsers(pageable).map(UserResponse::new);
        return ResponseEntity.ok(usersResponse);
    }

    /**
     * Obtiene los detalles de un usuario específico por su ID.
     *
     * @param id ID del usuario.
     * @return Detalles del usuario.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.debug("GET /api/admin/users/{}", id);
        User user = userService.findUserById(id);
        return ResponseEntity.ok(new UserResponse(user));
    }

    /**
     * Obtiene una lista de usuarios filtrados por su rol.
     *
     * @param roleName Nombre del rol (ej. 'ADMIN', 'OPERARIO').
     * @return Lista de usuarios con ese rol.
     */
    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String roleName) {
        log.debug("GET /api/admin/users/role/{}", roleName);
        List<UserResponse> users = userService.findUsersByRole(roleName).stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * Actualiza la información general de un usuario.
     *
     * @param id ID del usuario a actualizar.
     * @param updateRequest Datos nuevos (nombre, apellido, email, rol).
     * @return Usuario actualizado.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserUpdateRequest updateRequest) {
        log.debug("PUT /api/admin/users/{}", id);
        User updated = userService.updateUser(id, updateRequest);
        return ResponseEntity.ok(new UserResponse(updated));
    }

    /**
     * Actualiza el estado (Activo/Inactivo) de un usuario.
     *
     * @param id ID del usuario.
     * @param statusRequest Objeto que contiene el nuevo estado booleano.
     * @return Usuario con el estado actualizado.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long id,
                                                         @Valid @RequestBody UserStatusUpdateRequest statusRequest) {
        log.debug("PUT /api/admin/users/{}/status", id);
        User updated = userService.updateUserStatus(id, statusRequest.getActive());
        return ResponseEntity.ok(new UserResponse(updated));
    }

    /**
     * Permite a un administrador cambiar la contraseña de cualquier usuario sin conocer la anterior.
     *
     * @param id ID del usuario.
     * @param req DTO con la nueva contraseña.
     * @return Mensaje de confirmación.
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updatePassword(@PathVariable Long id,
                                                 @Valid @RequestBody AdminPasswordUpdateRequest req) {
        log.debug("PUT /api/admin/users/{}/password", id);
        userService.updatePassword(id, req.getNewPassword());
        return ResponseEntity.ok("Contraseña actualizada exitosamente.");
    }

    /**
     * Elimina un usuario del sistema.
     *
     * @param id ID del usuario a eliminar.
     * @return 204 No Content.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.debug("DELETE /api/admin/users/{}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Asigna un usuario específico a una finca específica.
     *
     * @param userId ID del usuario.
     * @param farmId ID de la finca.
     * @return Mensaje de confirmación.
     */
    @PostMapping("/{userId}/farms/{farmId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignUserToFarm(@PathVariable Long userId, @PathVariable Integer farmId) {
        log.info("Solicitud POST para asignar usuario ID {} a finca ID {}", userId, farmId);
        userService.assignUserToFarm(userId, farmId);
        return ResponseEntity.ok(String.format("Usuario con ID %d asignado exitosamente a la finca con ID %d.", userId, farmId));
    }

    /**
     * Desasigna un usuario específico de una finca específica.
     *
     * @param userId ID del usuario.
     * @param farmId ID de la finca.
     * @return 204 No Content.
     */
    @DeleteMapping("/{userId}/farms/{farmId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unassignUserFromFarm(@PathVariable Long userId, @PathVariable Integer farmId) {
        log.info("Solicitud DELETE para desasignar usuario ID {} de la finca ID {}", userId, farmId);
        userService.unassignUserFromFarm(userId, farmId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene una lista de todos los usuarios asignados a una finca específica.
     *
     * @param farmId ID de la finca.
     * @return Lista de usuarios.
     */
    @GetMapping("/farms/{farmId}/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA')")
    public ResponseEntity<List<UserResponse>> getUsersAssignedToFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener usuarios de la finca ID {}", farmId);
        List<UserResponse> users = userService.findUsersByFarm(farmId).stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}