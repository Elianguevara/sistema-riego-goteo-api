package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.admin;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.UserStatsResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserService userService;

    /**
     * Obtiene estadísticas generales de los usuarios para el dashboard de administración.
     *
     * @return Objeto con conteo de usuarios por rol y estado.
     */
    @GetMapping("/user-stats")
    public ResponseEntity<UserStatsResponse> getUserStatistics() {
        UserStatsResponse stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }
}