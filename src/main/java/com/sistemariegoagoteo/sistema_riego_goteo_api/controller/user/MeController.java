
package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.user;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.SelfPasswordUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.FarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j; // <-- Importación necesaria

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/me") // Ruta base para el usuario actual
@RequiredArgsConstructor
@Slf4j // <-- Anotación añadida
@PreAuthorize("isAuthenticated()") // Requiere que el usuario esté logueado para todos los endpoints
public class MeController {

    private final UserService userService;
    private final FarmService farmService;

    // Endpoint 1: GET /api/me
    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new UserResponse(currentUser));
    }

    // Endpoint 2: PUT /api/me/password
    @PutMapping("/password")
    public ResponseEntity<String> updateCurrentUserPassword(@Valid @RequestBody SelfPasswordUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            // Llamar al servicio con todos los campos del DTO
            userService.updateOwnPassword(
                    username,
                    request.getCurrentPassword(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );
            return ResponseEntity.ok("Contraseña actualizada exitosamente.");

        } catch (BadCredentialsException e) {
            // Error si la contraseña actual es incorrecta
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());

        } catch (IllegalArgumentException e) {
            // Error si la nueva contraseña y la confirmación no coinciden
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (ResourceNotFoundException e) {
            // Caso raro, pero cubierto
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint 3: GET /api/me/farms
    @GetMapping("/farms")
    public ResponseEntity<List<FarmResponse>> getCurrentUserFarms() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // El método findFarmsByUsername debe ser creado en FarmService
        List<FarmResponse> farmResponses = farmService.findFarmsByUsername(username).stream()
                .map(FarmResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(farmResponses);
    }
    @PutMapping("/profile")
    public ResponseEntity<?> updateCurrentUserProfile(@Valid @RequestBody UserUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            User updatedUser = userService.updateOwnProfile(username, request);
            return ResponseEntity.ok(new UserResponse(updatedUser));
        } catch (RuntimeException e) {
            log.error("Error al actualizar perfil para {}: {}", username, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}