package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.config;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.config.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "System Configuration (Admin)", description = "Permite a los administradores gestionar dinámicamente los parámetros del motor de riego y la organización.")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    // --- 1. PARAMETROS AGRONÓMICOS ---
    @GetMapping("/agronomic")
    @Operation(summary = "Leer parámetros agronómicos (coeficientes, horas máximas de riego, umbrales)")
    public ResponseEntity<AgronomicConfigDTO> getAgronomicConfig() {
        return ResponseEntity.ok(systemConfigService.getAgronomicConfig());
    }

    @PutMapping("/agronomic")
    @Operation(summary = "Actualizar parámetros agronómicos")
    public ResponseEntity<AgronomicConfigDTO> updateAgronomicConfig(
            @Valid @RequestBody AgronomicConfigDTO dto,
            @AuthenticationPrincipal User adminUser) {
        return ResponseEntity.ok(systemConfigService.updateAgronomicConfig(dto, adminUser));
    }

    // --- 2. DATOS DE LA ORGANIZACIÓN ---
    @GetMapping("/organization")
    @Operation(summary = "Leer datos corporativos (logo, teléfonos y direcciones para los PDF)")
    public ResponseEntity<OrganizationConfigDTO> getOrganizationConfig() {
        return ResponseEntity.ok(systemConfigService.getOrganizationConfig());
    }

    @PutMapping("/organization")
    @Operation(summary = "Actualizar datos corporativos")
    public ResponseEntity<OrganizationConfigDTO> updateOrganizationConfig(
            @Valid @RequestBody OrganizationConfigDTO dto,
            @AuthenticationPrincipal User adminUser) {
        return ResponseEntity.ok(systemConfigService.updateOrganizationConfig(dto, adminUser));
    }

    // --- 3. SEGURIDAD Y SESIONES ---
    @GetMapping("/security")
    @Operation(summary = "Leer configuración global de durabilidad de token e intentos de Logins")
    public ResponseEntity<SecurityConfigDTO> getSecurityConfig() {
        return ResponseEntity.ok(systemConfigService.getSecurityConfig());
    }

    @PutMapping("/security")
    @Operation(summary = "Actualizar reglas de seguridad, contraseñas y sesiones JWT")
    public ResponseEntity<SecurityConfigDTO> updateSecurityConfig(
            @Valid @RequestBody SecurityConfigDTO dto,
            @AuthenticationPrincipal User adminUser) {
        return ResponseEntity.ok(systemConfigService.updateSecurityConfig(dto, adminUser));
    }

    // --- 4. NOTIFICACIONES ---
    @GetMapping("/notifications")
    @Operation(summary = "Leer qué canales de alerta tipo IRRIGATION o WEATHER están habilitados")
    public ResponseEntity<NotificationConfigDTO> getNotificationConfig() {
        return ResponseEntity.ok(systemConfigService.getNotificationConfig());
    }

    @PutMapping("/notifications")
    @Operation(summary = "Habilitar/deshabilitar los canales de notificación global o específicos")
    public ResponseEntity<NotificationConfigDTO> updateNotificationConfig(
            @Valid @RequestBody NotificationConfigDTO dto,
            @AuthenticationPrincipal User adminUser) {
        return ResponseEntity.ok(systemConfigService.updateNotificationConfig(dto, adminUser));
    }

    // --- 5. REPORTES ---
    @GetMapping("/reports")
    @Operation(summary = "Leer las reglas de retención y formato automático de los archivos exportados")
    public ResponseEntity<ReportConfigDTO> getReportConfig() {
        return ResponseEntity.ok(systemConfigService.getReportConfig());
    }

    @PutMapping("/reports")
    @Operation(summary = "Actualizar retención y configuración de expiración de analítica")
    public ResponseEntity<ReportConfigDTO> updateReportConfig(
            @Valid @RequestBody ReportConfigDTO dto,
            @AuthenticationPrincipal User adminUser) {
        return ResponseEntity.ok(systemConfigService.updateReportConfig(dto, adminUser));
    }

    // --- 6. SERVICIOS METEOROLÓGICOS EXTERNOSS ---
    @GetMapping("/weather")
    @Operation(summary = "Leer intervalo del WeatherService. Devuelve un API KEY OFUSCADA por seguridad (sk-****)")
    public ResponseEntity<WeatherConfigDTO> getWeatherConfig() {
        return ResponseEntity.ok(systemConfigService.getWeatherConfig());
    }

    @PutMapping("/weather")
    @Operation(summary = "Configurar proveedor del clima. Si actualizas la API KEY entexto plano de cambiará.")
    public ResponseEntity<WeatherConfigDTO> updateWeatherConfig(
            @Valid @RequestBody WeatherConfigDTO dto,
            @AuthenticationPrincipal User adminUser) {
        return ResponseEntity.ok(systemConfigService.updateWeatherConfig(dto, adminUser));
    }
}
