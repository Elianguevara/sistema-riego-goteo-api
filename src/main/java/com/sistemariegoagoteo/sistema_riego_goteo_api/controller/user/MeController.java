package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.user;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.SelfPasswordUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.FarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class MeController {

    private final UserService userService;
    private final FarmService farmService;

    /**
     * Obtiene el perfil del usuario autenticado.
     *
     * @return Datos del usuario.
     */
    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new UserResponse(currentUser));
    }

    /**
     * Actualiza la contraseña del usuario autenticado.
     * Requiere la contraseña actual para validación.
     *
     * @param request Datos de cambio de contraseña.
     * @return Mensaje de éxito.
     */
    @PutMapping("/password")
    public ResponseEntity<String> updateCurrentUserPassword(@Valid @RequestBody SelfPasswordUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Las excepciones (BadCredentialsException, IllegalArgumentException) se manejan globalmente
        userService.updateOwnPassword(
                username,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok("Contraseña actualizada exitosamente.");
    }

    /**
     * Obtiene la lista de fincas asignadas al usuario autenticado.
     *
     * @return Lista de fincas.
     */
    @GetMapping("/farms")
    public ResponseEntity<List<FarmResponse>> getCurrentUserFarms() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<FarmResponse> farmResponses = farmService.findFarmsByUsername(username).stream()
                .map(FarmResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(farmResponses);
    }

    /**
     * Actualiza el perfil (nombre, email, etc.) del usuario autenticado.
     *
     * @param request Datos a actualizar.
     * @return Usuario actualizado.
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateCurrentUserProfile(@Valid @RequestBody UserUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User updatedUser = userService.updateOwnProfile(username, request);
        return ResponseEntity.ok(new UserResponse(updatedUser));
    }
}