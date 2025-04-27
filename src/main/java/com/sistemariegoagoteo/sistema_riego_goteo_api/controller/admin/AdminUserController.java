package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.admin;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserStatusUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException; // Importar excepción personalizada
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
 * Controlador REST para que los Administradores gestionen otros usuarios.
 * Todos los endpoints aquí requieren autenticación y rol ADMIN.
 */
@RestController
@RequestMapping("/api/admin/users") // Ruta base para la gestión de usuarios por admin
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')") // Aplica a todos los métodos de esta clase
public class AdminUserController {

    private final UserService userService;

    /**
     * Obtiene una lista de todos los usuarios.
     * @return ResponseEntity con la lista de UserResponse y estado OK.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.debug("Request GET /api/admin/users recibida");
        List<User> users = userService.findAllUsers();
        // Mapear entidades User a UserResponse DTOs para no exponer contraseñas, etc.
        List<UserResponse> userResponses = users.stream()
                                                .map(UserResponse::new) // Usa el constructor del DTO
                                                .collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    /**
     * Obtiene un usuario específico por su ID.
     * @param id El ID del usuario.
     * @return ResponseEntity con UserResponse y estado OK si se encuentra, o NOT_FOUND si no.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
         log.debug("Request GET /api/admin/users/{} recibida", id);
        try {
            User user = userService.findUserById(id);
            return ResponseEntity.ok(new UserResponse(user));
        } catch (ResourceNotFoundException e) {
             log.warn("Usuario no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

     /**
     * Obtiene una lista de usuarios filtrados por nombre de rol.
     * @param roleName Nombre del rol (ej. "ANALISTA", "OPERARIO").
     * @return ResponseEntity con la lista de UserResponse y estado OK, o NOT_FOUND si el rol no existe.
     */
    @GetMapping("/role/{roleName}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String roleName) {
        log.debug("Request GET /api/admin/users/role/{} recibida", roleName);
         try {
             List<User> users = userService.findUsersByRole(roleName);
             List<UserResponse> userResponses = users.stream()
                                                    .map(UserResponse::new)
                                                    .collect(Collectors.toList());
             return ResponseEntity.ok(userResponses);
         } catch (ResourceNotFoundException e) {
             log.warn("Rol no encontrado: {}", roleName);
             // Podrías devolver 404 o 400 Bad Request si el rol no es válido
             return ResponseEntity.notFound().build();
         }
    }


    /**
     * Actualiza los datos (nombre, email) de un usuario.
     * @param id El ID del usuario a actualizar.
     * @param updateRequest DTO con los datos a actualizar.
     * @return ResponseEntity con UserResponse actualizado y estado OK,
     * o NOT_FOUND si el usuario no existe, o BAD_REQUEST si el email ya está en uso.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest updateRequest) {
        log.debug("Request PUT /api/admin/users/{} recibida", id);
        try {
            User updatedUser = userService.updateUser(id, updateRequest);
            return ResponseEntity.ok(new UserResponse(updatedUser));
        } catch (ResourceNotFoundException e) {
             log.warn("Usuario no encontrado para actualizar con ID: {}", id);
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (RuntimeException e) { // Captura error de email duplicado
            log.error("Error al actualizar usuario {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage()); // 400 Bad Request
        }
    }

    /**
     * Actualiza el estado (activo/inactivo) de un usuario.
     * @param id El ID del usuario.
     * @param statusRequest DTO con el nuevo estado 'activo'.
     * @return ResponseEntity con UserResponse actualizado y estado OK, o NOT_FOUND si el usuario no existe.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest statusRequest) {
         log.debug("Request PUT /api/admin/users/{}/status recibida", id);
         try {
            User updatedUser = userService.updateUserStatus(id, statusRequest.getActivo());
            return ResponseEntity.ok(new UserResponse(updatedUser));
        } catch (ResourceNotFoundException e) {
             log.warn("Usuario no encontrado para actualizar estado con ID: {}", id);
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

    /**
     * Elimina permanentemente un usuario.
     * @param id El ID del usuario a eliminar.
     * @return ResponseEntity con estado NO_CONTENT si se elimina, o NOT_FOUND si no existe.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.debug("Request DELETE /api/admin/users/{} recibida", id);
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (ResourceNotFoundException e) {
             log.warn("Usuario no encontrado para eliminar con ID: {}", id);
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
        // Considerar capturar DataIntegrityViolationException si hay FKs que impiden borrar
    }
}
