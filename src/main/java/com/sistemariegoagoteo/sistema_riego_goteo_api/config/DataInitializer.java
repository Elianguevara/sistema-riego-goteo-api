package com.sistemariegoagoteo.sistema_riego_goteo_api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import net.datafaker.Faker;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Componente que se ejecuta al iniciar la aplicación para inicializar la BD.
 * Realiza una inyección de datos "Data Seeding" para pruebas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    // Repositorios de Usuario
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Repositorios de Entidades de Granja
    private final FarmRepository farmRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationEquipmentRepository equipmentRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final IrrigationRepository irrigationRepository;
    private final TaskRepository taskRepository;

    private static final List<String> ROLES_ESENCIALES = Arrays.asList("ADMIN", "ANALISTA", "OPERARIO");

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Iniciando verificación e inicialización de roles esenciales...");
        initRoles();
        initDefaultAdmin();

        // Idempotencia aditiva: Verificamos si existe el usuario marcador
        // "system_seeder"
        if (!userRepository.existsByUsername("system_seeder")) {
            log.info(
                    "Marcador 'system_seeder' no encontrado. Comenzando el sembrado de datos (Data Seeding) histórico...");
            try {
                seedData();
                log.info("¡Sembrado de datos masivo completado exitosamente!");
            } catch (Exception e) {
                log.error("Excepción al ejecutar el seeder de datos: ", e);
            }
        } else {
            log.info(
                    "El sembrado de datos base ya se había inyectado anteriormente. Operación omitida para no duplicar.");
        }
    }

    private void initRoles() {
        for (String roleName : ROLES_ESENCIALES) {
            if (!roleRepository.findByRoleName(roleName).isPresent()) {
                roleRepository.save(new Role(roleName));
                log.info("Rol '{}' creado exitosamente.", roleName);
            }
        }
    }

    private void initDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByRoleName("ADMIN").orElseThrow();
            User defaultAdmin = new User(
                    "Elian Guevara",
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "elian.guevara689@gmail.com",
                    adminRole);
            userRepository.save(defaultAdmin);
            log.info("Usuario administrador por defecto 'admin' creado.");
        }
    }

    private void seedData() {
        Faker faker = new Faker(new Locale("es"));

        // 1. Obtención de Roles
        Role operarioRole = roleRepository.findByRoleName("OPERARIO").orElseThrow();
        Role analistaRole = roleRepository.findByRoleName("ANALISTA").orElseThrow();

        // 2. Creación del Marcador para asegurar la consistencia en el futuro
        User seeder = new User(
                "System Seeder",
                "system_seeder",
                passwordEncoder.encode("secret_seeder"),
                "seeder@sistema.local",
                operarioRole);
        userRepository.save(seeder);

        // 3. Capturar y generar analistas/operarios
        List<User> analistas = new ArrayList<>();
        List<User> operarios = new ArrayList<>();

        // Conservar a los que ya existen para usarlos en el reparto de tareas (sin
        // sobreescribir claves)
        userRepository.findAll().forEach(u -> {
            if ("ANALISTA".equals(u.getRol().getRoleName()))
                analistas.add(u);
            if ("OPERARIO".equals(u.getRol().getRoleName()))
                operarios.add(u);
        });

        // Aseguramos que haya volumen: Generar 1 Analista adicional con datos latinos
        // realistas
        User nuevoAnalista = new User(
                faker.name().fullName(),
                faker.internet().username().toLowerCase() + "_ana",
                passwordEncoder.encode("123456"),
                faker.internet().emailAddress(),
                analistaRole);
        analistas.add(userRepository.save(nuevoAnalista));

        // Generar 3 Operarios adicionales
        for (int i = 0; i < 3; i++) {
            User op = new User(
                    faker.name().fullName(),
                    faker.internet().username().toLowerCase(),
                    passwordEncoder.encode("123456"),
                    faker.internet().emailAddress(),
                    operarioRole);
            operarios.add(userRepository.save(op));
        }

        log.info("Se han generado y consolidado {} analistas y {} operarios en total.", analistas.size(),
                operarios.size());

        // 4. Crear Fincas (Farms)
        for (int f = 1; f <= 2; f++) {
            Farm farm = new Farm();
            farm.setName("Finca " + faker.address().cityName());
            farm.setLocation(faker.address().fullAddress());
            farm.setFarmSize(BigDecimal.valueOf(faker.number().randomDouble(2, 50, 500)));
            farm.setReservoirCapacity(BigDecimal.valueOf(faker.number().randomDouble(2, 1000, 5000)));
            farm.setLatitude(BigDecimal.valueOf(Double.parseDouble(faker.address().latitude().replace(',', '.'))));
            farm.setLongitude(BigDecimal.valueOf(Double.parseDouble(faker.address().longitude().replace(',', '.'))));
            farm = farmRepository.save(farm);

            // Fuente de Agua
            WaterSource ws = new WaterSource();
            ws.setFarm(farm);
            ws.setType(faker.options().option("POZO", "REPRESA", "CANAL"));
            waterSourceRepository.save(ws);

            // Asignar los operarios a la finca
            for (User op : operarios) {
                op.getFarms().add(farm);
                userRepository.save(op);
            }

            // 5. Crear 3 Sectores por Finca
            for (int s = 1; s <= 3; s++) {
                Sector sector = new Sector();
                sector.setFarm(farm);
                sector.setName("Lote " + faker.color().name().toUpperCase() + " " + s);
                sector = sectorRepository.save(sector);

                // Equipo de Riego
                IrrigationEquipment eq = new IrrigationEquipment();
                eq.setFarm(farm);
                eq.setName("Bomba " + faker.address().cityName() + " " + s);
                eq.setEquipmentType("Goteo");
                eq.setEquipmentStatus("Activo");
                eq.setMeasuredFlow(BigDecimal.valueOf(faker.number().randomDouble(2, 5, 20)));
                eq.setHasFlowMeter(faker.bool().bool());
                eq = equipmentRepository.save(eq);

                sector.setEquipment(eq);
                sectorRepository.save(sector);

                // 6. Generar Riegos Históricos (últimos 30 días)
                LocalDateTime now = LocalDateTime.now();
                int riegoCount = 0;
                for (int d = 30; d >= 0; d--) {
                    // Probabilidad de 50% de que se haya regado ese día
                    if (faker.number().numberBetween(1, 100) <= 50) {
                        LocalDateTime start = now.minusDays(d).withHour(faker.number().numberBetween(5, 18))
                                .withMinute(0);
                        int hours = faker.number().numberBetween(1, 5);
                        LocalDateTime end = start.plusHours(hours);

                        Irrigation iri = new Irrigation();
                        iri.setSector(sector);
                        iri.setEquipment(eq);
                        iri.setStartDatetime(start);
                        iri.setEndDatetime(end);
                        iri.setIrrigationHours(BigDecimal.valueOf(hours));
                        iri.setWaterAmount(BigDecimal.valueOf(hours * faker.number().randomDouble(2, 10, 25)));
                        irrigationRepository.save(iri); // El PrePersist asigna el UUID localMobileId
                        riegoCount++;
                    }
                }

                // 7. Generar Tareas Históricas Asignadas
                int taskCount = 0;
                for (int t = 0; t < 5; t++) {
                    Task task = new Task();
                    task.setSector(sector);
                    task.setDescription(faker.options().option(
                            "Revisión de cinta de goteo - Lote " + s,
                            "Mantenimiento preventivo bomba " + eq.getName(),
                            "Purga de tubería secundaria",
                            "Inspección de válvulas solenoides",
                            "Reparación de filtración"));

                    // Elegir un usuario aleatorio dentro de los existentes+nuevos
                    User randomAna = analistas.get(faker.number().numberBetween(0, analistas.size()));
                    User randomOpe = operarios.get(faker.number().numberBetween(0, operarios.size()));

                    task.setCreatedBy(randomAna);
                    task.setAssignedTo(randomOpe);
                    task.setStatus(faker.options().option(TaskStatus.class));

                    // Fechas dispares en el último mes
                    Date pastDate = faker.date().past(30, TimeUnit.DAYS);
                    task.setCreatedAt(pastDate);

                    if (task.getStatus() == TaskStatus.COMPLETADA || task.getStatus() == TaskStatus.CANCELADA) {
                        task.setUpdatedAt(faker.date().between(pastDate, new Date()));
                    }

                    taskRepository.save(task);
                    taskCount++;
                }

                log.info("Generado Sector '{}' -> Riegos: {}, Tareas: {}", sector.getName(), riegoCount, taskCount);
            }
        }
    }
}
