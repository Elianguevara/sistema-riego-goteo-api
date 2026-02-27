package com.sistemariegoagoteo.sistema_riego_goteo_api.config.seeder;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.config.ConfigType;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.config.SystemConfig;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.config.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SystemConfigSeeder implements CommandLineRunner {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Iniciando SystemConfigSeeder...");

        if (systemConfigRepository.count() > 0) {
            log.info("La tabla system_config ya contiene datos. Saltando seeding.");
            return;
        }

        List<SystemConfig> defaultConfigs = Arrays.asList(
                // AGRONOMIC
                new SystemConfig("AGRONOMIC_EFF_RAIN_COEF", "0.80", ConfigType.FLOAT, "Coeficiente de lluvia efectiva"),
                new SystemConfig("AGRONOMIC_MAX_IRRIG_HOURS", "12", ConfigType.INTEGER,
                        "Máximo de horas de riego por día"),
                new SystemConfig("AGRONOMIC_MIN_IRRIG_INTERVAL", "4", ConfigType.INTEGER,
                        "Intervalo mínimo de riego (h)"),
                new SystemConfig("AGRONOMIC_PRECIP_THRESH_MM", "5.0", ConfigType.FLOAT,
                        "Umbral de lluvia efectiva (mm)"),
                new SystemConfig("AGRONOMIC_RES_LOW_THRESH_PCT", "20", ConfigType.INTEGER,
                        "Umbral bajo de reservorio (%)"),

                // ORGANIZATION
                new SystemConfig("ORG_NAME", "Empresa Agro S.A.", ConfigType.STRING, "Nombre de la Organización"),
                new SystemConfig("ORG_ADDRESS", "Ruta 40 Km 120, Mendoza", ConfigType.STRING,
                        "Dirección de la Organización"),
                new SystemConfig("ORG_PHONE", "+54 261 123-4567", ConfigType.STRING, "Teléfono de la Organización"),
                new SystemConfig("ORG_EMAIL", "contacto@empresa.com", ConfigType.STRING, "Email de la Organización"),

                // SECURITY
                new SystemConfig("SEC_SESSION_DUR_HOURS", "8", ConfigType.INTEGER, "Duración de sesión (horas)"),
                new SystemConfig("SEC_MAX_FAIL_LOGIN", "5", ConfigType.INTEGER, "Máx. intentos fallidos login"),
                new SystemConfig("SEC_PASS_MIN_LEN", "8", ConfigType.INTEGER, "Longitud mínima de contraseña"),
                new SystemConfig("SEC_FORCE_PASS_CHANGE", "true", ConfigType.BOOLEAN, "Forzar cambio de password"),

                // NOTIFICATIONS
                new SystemConfig("NOTIF_GLOBAL_ENABLE", "true", ConfigType.BOOLEAN, "Activar notificaciones globales"),
                new SystemConfig("NOTIF_CHANNELS",
                        "{\"IRRIGATION\":{\"enabled\":true},\"TASK\":{\"enabled\":true},\"HUMEDAD\":{\"enabled\":true},\"PRECIPITATION\":{\"enabled\":true},\"FARM\":{\"enabled\":true},\"REPORT\":{\"enabled\":true},\"GENERAL\":{\"enabled\":true}}",
                        ConfigType.JSON, "Configuración de canales (JSON)"),

                // REPORTS
                new SystemConfig("REP_RETENTION_DAYS", "30", ConfigType.INTEGER, "Días de retención de reportes"),
                new SystemConfig("REP_MAX_DATE_RANGE_MONTHS", "12", ConfigType.INTEGER,
                        "Rango máximo de fechas (meses)"),
                new SystemConfig("REP_DEFAULT_FMT", "PDF", ConfigType.STRING, "Formato por defecto"),

                // WEATHER
                new SystemConfig("WTH_ENABLED", "true", ConfigType.BOOLEAN, "Servicio de clima activo"),
                new SystemConfig("WTH_UPDATE_INTERVAL_MIN", "15", ConfigType.INTEGER,
                        "Intervalo de actualización clima (min)"),
                new SystemConfig("WTH_PROVIDER", "OPENWEATHERMAP", ConfigType.STRING, "Proveedor de clima"),
                new SystemConfig("WTH_API_KEY", "", ConfigType.STRING, "API Key del clima"));

        systemConfigRepository.saveAll(defaultConfigs);
        log.info("SystemConfigSeeder finalizado. Insertados {} registros en system_config.", defaultConfigs.size());
    }
}
