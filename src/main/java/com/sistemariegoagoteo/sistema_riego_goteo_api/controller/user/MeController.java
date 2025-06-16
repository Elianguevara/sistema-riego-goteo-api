
package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.user;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.PasswordUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.FarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/me") // Ruta base para el usuario actual
@RequiredArgsConstructor
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
    public ResponseEntity<String> updateCurrentUserPassword(@Valid @RequestBody PasswordUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        try {
            userService.updateOwnPassword(username, request.getNewPassword());
            return ResponseEntity.ok("Contraseña actualizada exitosamente.");
        } catch (ResourceNotFoundException e) {
            // Este caso es muy raro si el usuario está autenticado, pero es buena práctica manejarlo.
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