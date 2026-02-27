package com.sistemariegoagoteo.sistema_riego_goteo_api.service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.config.ConfigType;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.config.SystemConfig;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.config.SystemConfigRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // --- AGRONOMIC ---
    public AgronomicConfigDTO getAgronomicConfig() {
        AgronomicConfigDTO dto = new AgronomicConfigDTO();
        dto.setEffectiveRainCoefficient(getFloatValue("AGRONOMIC_EFF_RAIN_COEF", 0.80f));
        dto.setMaxIrrigationHoursPerDay(getIntValue("AGRONOMIC_MAX_IRRIG_HOURS", 12));
        dto.setMinIrrigationIntervalHours(getIntValue("AGRONOMIC_MIN_IRRIG_INTERVAL", 4));
        dto.setPrecipitationEffectivenessThresholdMm(getFloatValue("AGRONOMIC_PRECIP_THRESH_MM", 5.0f));
        dto.setReservoirLowThresholdPercent(getIntValue("AGRONOMIC_RES_LOW_THRESH_PCT", 20));
        return dto;
    }

    @Transactional
    public AgronomicConfigDTO updateAgronomicConfig(AgronomicConfigDTO dto, User admin) {
        updateConfig("AGRONOMIC_EFF_RAIN_COEF", String.valueOf(dto.getEffectiveRainCoefficient()), ConfigType.FLOAT,
                "Coeficiente de lluvia efectiva", admin);
        updateConfig("AGRONOMIC_MAX_IRRIG_HOURS", String.valueOf(dto.getMaxIrrigationHoursPerDay()), ConfigType.INTEGER,
                "Máximo de horas de riego por día", admin);
        updateConfig("AGRONOMIC_MIN_IRRIG_INTERVAL", String.valueOf(dto.getMinIrrigationIntervalHours()),
                ConfigType.INTEGER, "Intervalo mínimo de riego (h)", admin);
        updateConfig("AGRONOMIC_PRECIP_THRESH_MM", String.valueOf(dto.getPrecipitationEffectivenessThresholdMm()),
                ConfigType.FLOAT, "Umbral de lluvia efectiva (mm)", admin);
        updateConfig("AGRONOMIC_RES_LOW_THRESH_PCT", String.valueOf(dto.getReservoirLowThresholdPercent()),
                ConfigType.INTEGER, "Umbral bajo de reservorio (%)", admin);
        return dto;
    }

    // --- ORGANIZATION ---
    public OrganizationConfigDTO getOrganizationConfig() {
        OrganizationConfigDTO dto = new OrganizationConfigDTO();
        dto.setOrganizationName(getStringValue("ORG_NAME", "Empresa Agro S.A."));
        dto.setOrganizationAddress(getStringValue("ORG_ADDRESS", "Ruta 40 Km 120, Mendoza"));
        dto.setOrganizationPhone(getStringValue("ORG_PHONE", "+54 261 123-4567"));
        dto.setOrganizationEmail(getStringValue("ORG_EMAIL", "contacto@empresa.com"));
        return dto;
    }

    @Transactional
    public OrganizationConfigDTO updateOrganizationConfig(OrganizationConfigDTO dto, User admin) {
        updateConfig("ORG_NAME", dto.getOrganizationName(), ConfigType.STRING, "Nombre de la Organización", admin);
        updateConfig("ORG_ADDRESS", dto.getOrganizationAddress(), ConfigType.STRING, "Dirección de la Organización",
                admin);
        updateConfig("ORG_PHONE", dto.getOrganizationPhone(), ConfigType.STRING, "Teléfono de la Organización", admin);
        updateConfig("ORG_EMAIL", dto.getOrganizationEmail(), ConfigType.STRING, "Email de la Organización", admin);
        return dto;
    }

    // --- SECURITY ---
    public SecurityConfigDTO getSecurityConfig() {
        SecurityConfigDTO dto = new SecurityConfigDTO();
        dto.setSessionDurationHours(getIntValue("SEC_SESSION_DUR_HOURS", 8));
        dto.setMaxFailedLoginAttempts(getIntValue("SEC_MAX_FAIL_LOGIN", 5));
        dto.setPasswordMinLength(getIntValue("SEC_PASS_MIN_LEN", 8));
        dto.setForcePasswordChangeOnFirstLogin(getBooleanValue("SEC_FORCE_PASS_CHANGE", true));
        return dto;
    }

    @Transactional
    public SecurityConfigDTO updateSecurityConfig(SecurityConfigDTO dto, User admin) {
        updateConfig("SEC_SESSION_DUR_HOURS", String.valueOf(dto.getSessionDurationHours()), ConfigType.INTEGER,
                "Duración de sesión (horas)", admin);
        updateConfig("SEC_MAX_FAIL_LOGIN", String.valueOf(dto.getMaxFailedLoginAttempts()), ConfigType.INTEGER,
                "Máx. intentos fallidos login", admin);
        updateConfig("SEC_PASS_MIN_LEN", String.valueOf(dto.getPasswordMinLength()), ConfigType.INTEGER,
                "Longitud mínima de contraseña", admin);
        updateConfig("SEC_FORCE_PASS_CHANGE", String.valueOf(dto.getForcePasswordChangeOnFirstLogin()),
                ConfigType.BOOLEAN, "Forzar cambio de password", admin);
        return dto;
    }

    // --- NOTIFICATIONS ---
    public NotificationConfigDTO getNotificationConfig() {
        NotificationConfigDTO dto = new NotificationConfigDTO();
        dto.setGlobalNotificationsEnabled(getBooleanValue("NOTIF_GLOBAL_ENABLE", true));

        String defaultChannels = "{\"IRRIGATION\":{\"enabled\":true},\"TASK\":{\"enabled\":true},\"HUMEDAD\":{\"enabled\":true},\"PRECIPITATION\":{\"enabled\":true},\"FARM\":{\"enabled\":true},\"REPORT\":{\"enabled\":true},\"GENERAL\":{\"enabled\":true}}";
        String channelsJson = getStringValue("NOTIF_CHANNELS", defaultChannels);

        try {
            Map<String, NotificationConfigDTO.ChannelConfig> channels = objectMapper.readValue(channelsJson,
                    new TypeReference<Map<String, NotificationConfigDTO.ChannelConfig>>() {
                    });
            dto.setChannels(channels);
        } catch (JsonProcessingException e) {
            log.error("Error parseando canales de notificación", e);
            dto.setChannels(Map.of());
        }
        return dto;
    }

    @Transactional
    public NotificationConfigDTO updateNotificationConfig(NotificationConfigDTO dto, User admin) {
        updateConfig("NOTIF_GLOBAL_ENABLE", String.valueOf(dto.getGlobalNotificationsEnabled()), ConfigType.BOOLEAN,
                "Activar notificaciones globales", admin);
        try {
            String channelsJson = objectMapper.writeValueAsString(dto.getChannels());
            updateConfig("NOTIF_CHANNELS", channelsJson, ConfigType.JSON, "Configuración de canales (JSON)", admin);
        } catch (JsonProcessingException e) {
            log.error("Error serializando canales de notificación", e);
            throw new RuntimeException("Error serializando configuración de notificaciones", e);
        }
        return dto;
    }

    // --- REPORTS ---
    public ReportConfigDTO getReportConfig() {
        ReportConfigDTO dto = new ReportConfigDTO();
        dto.setReportRetentionDays(getIntValue("REP_RETENTION_DAYS", 30));
        dto.setMaxReportDateRangeMonths(getIntValue("REP_MAX_DATE_RANGE_MONTHS", 12));
        dto.setDefaultReportFormat(getStringValue("REP_DEFAULT_FMT", "PDF"));
        return dto;
    }

    @Transactional
    public ReportConfigDTO updateReportConfig(ReportConfigDTO dto, User admin) {
        updateConfig("REP_RETENTION_DAYS", String.valueOf(dto.getReportRetentionDays()), ConfigType.INTEGER,
                "Días de retención de reportes", admin);
        updateConfig("REP_MAX_DATE_RANGE_MONTHS", String.valueOf(dto.getMaxReportDateRangeMonths()), ConfigType.INTEGER,
                "Rango máximo de fechas (meses)", admin);
        updateConfig("REP_DEFAULT_FMT", dto.getDefaultReportFormat(), ConfigType.STRING, "Formato por defecto", admin);
        return dto;
    }

    // --- WEATHER ---
    public WeatherConfigDTO getWeatherConfig() {
        WeatherConfigDTO dto = new WeatherConfigDTO();
        dto.setWeatherServiceEnabled(getBooleanValue("WTH_ENABLED", true));
        dto.setWeatherUpdateIntervalMinutes(getIntValue("WTH_UPDATE_INTERVAL_MIN", 15));
        dto.setWeatherProvider(getStringValue("WTH_PROVIDER", "OPENWEATHERMAP"));

        String apiKey = getStringValue("WTH_API_KEY", "");
        if (apiKey != null && apiKey.length() > 4) {
            dto.setWeatherApiKey("sk-****" + apiKey.substring(apiKey.length() - 4));
        } else {
            dto.setWeatherApiKey("");
        }

        return dto;
    }

    @Transactional
    public WeatherConfigDTO updateWeatherConfig(WeatherConfigDTO dto, User admin) {
        updateConfig("WTH_ENABLED", String.valueOf(dto.getWeatherServiceEnabled()), ConfigType.BOOLEAN,
                "Servicio de clima activo", admin);
        updateConfig("WTH_UPDATE_INTERVAL_MIN", String.valueOf(dto.getWeatherUpdateIntervalMinutes()),
                ConfigType.INTEGER, "Intervalo de actualización clima (min)", admin);
        updateConfig("WTH_PROVIDER", dto.getWeatherProvider(), ConfigType.STRING, "Proveedor de clima", admin);

        // Evitamos sobreescribir si envían la enmascarada
        if (dto.getWeatherApiKey() != null && !dto.getWeatherApiKey().isBlank()
                && !dto.getWeatherApiKey().startsWith("sk-****")) {
            updateConfig("WTH_API_KEY", dto.getWeatherApiKey(), ConfigType.STRING, "API Key del clima", admin);
        }

        return dto;
    }

    // --- HELPERS PARA LECTURA SEGURA (Con Defaults) ---
    private String getStringValue(String key, String defaultValue) {
        return systemConfigRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    private Integer getIntValue(String key, Integer defaultValue) {
        return systemConfigRepository.findById(key)
                .map(c -> Integer.parseInt(c.getConfigValue()))
                .orElse(defaultValue);
    }

    private Float getFloatValue(String key, Float defaultValue) {
        return systemConfigRepository.findById(key)
                .map(c -> Float.parseFloat(c.getConfigValue()))
                .orElse(defaultValue);
    }

    private Boolean getBooleanValue(String key, Boolean defaultValue) {
        return systemConfigRepository.findById(key)
                .map(c -> Boolean.parseBoolean(c.getConfigValue()))
                .orElse(defaultValue);
    }

    // --- HELPER PARA ACTUALIZACIÓN ESCRITURA Y AUDITORÍA ---
    private void updateConfig(String key, String newValue, ConfigType type, String description, User admin) {
        Optional<SystemConfig> existingOpt = systemConfigRepository.findById(key);
        String oldValue = existingOpt.map(SystemConfig::getConfigValue).orElse(null);

        if (oldValue == null || !oldValue.equals(newValue)) {
            SystemConfig config = existingOpt.orElseGet(() -> {
                SystemConfig newConfig = new SystemConfig();
                newConfig.setConfigKey(key);
                newConfig.setConfigType(type);
                newConfig.setDescription(description);
                return newConfig;
            });

            config.setConfigValue(newValue);
            systemConfigRepository.save(config);

            // Registrar en Auditoría
            if (admin != null) {
                String action = existingOpt.isPresent() ? "UPDATE" : "CREATE";
                auditService.logChange(admin, action, "system_config", key, oldValue == null ? "NULL" : oldValue,
                        newValue);
            }
        }
    }
}
