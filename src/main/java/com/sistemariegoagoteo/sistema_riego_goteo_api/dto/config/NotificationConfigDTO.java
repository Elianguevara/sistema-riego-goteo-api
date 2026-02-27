package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class NotificationConfigDTO {

    @NotNull(message = "Debe especificar si las notificaciones globales están activas")
    private Boolean globalNotificationsEnabled;

    @NotNull(message = "Debe proveer la configuración de los canales")
    private Map<String, ChannelConfig> channels;

    @Data
    public static class ChannelConfig {
        @NotNull(message = "Debe especificar si el canal está activo")
        private Boolean enabled;
    }
}
