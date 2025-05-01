package com.sistemariegoagoteo.sistema_riego_goteo_api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Opcional pero bueno para asegurar atomicidad

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

/**
 * Componente que se ejecuta al iniciar la aplicación para inicializar datos básicos.
 * Implementa CommandLineRunner, cuyo método run() se ejecuta después de que el
 * contexto de la aplicación Spring se haya cargado.
 * Aquí se utiliza para asegurar que los roles esenciales existan en la base de datos.
 */
@Component // Marca esta clase como un bean de Spring
@RequiredArgsConstructor // Lombok: genera constructor con campos final (roleRepository)
@Slf4j // Lombok: logger
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository; // Si necesitas crear un usuario por defecto
    private final PasswordEncoder passwordEncoder; // Para codificar contraseñas
    private final RoleRepository roleRepository; // Repositorio para gestionar roles

    // Lista de nombres de roles esenciales que deben existir
    private static final List<String> ROLES_ESENCIALES = Arrays.asList("ADMIN", "ANALISTA", "OPERARIO");

    /**
     * Este método se ejecuta automáticamente al iniciar la aplicación Spring Boot.
     * Verifica la existencia de los roles esenciales y los crea si no existen.
     *
     * @param args Argumentos de línea de comandos (no utilizados aquí).
     * @throws Exception Si ocurre algún error durante la inicialización.
     */
    @Override
    @Transactional // Ejecuta el método dentro de una transacción
    public void run(String... args) throws Exception {
        log.info("Iniciando verificación e inicialización de roles esenciales...");

        for (String nombreRol : ROLES_ESENCIALES) {
            // Verifica si el rol ya existe en la base de datos
            if (!roleRepository.findByNombreRol(nombreRol).isPresent()) {
                // Si no existe, crea una nueva instancia de Role
                Role nuevoRol = new Role(nombreRol);
                // Guarda el nuevo rol en la base de datos
                roleRepository.save(nuevoRol);
                log.info("Rol '{}' creado exitosamente.", nombreRol);
            } else {
                log.debug("El rol '{}' ya existe en la base de datos.", nombreRol); // Log a nivel debug si ya existe
            }
        }

        log.info("Verificación e inicialización de roles completada.");

        // Aquí podrías añadir más lógica de inicialización si fuera necesario,
        // como crear un usuario admin por defecto si no existe ninguno.
        // ¡Cuidado con las contraseñas hardcodeadas en producción!
        
        if (!userRepository.existsByUsername("admin")) {
             Role adminRole = roleRepository.findByNombreRol("ADMIN").orElseThrow();
             User defaultAdmin = new User("Elian Guevara",
             "admin", 
             passwordEncoder.encode("admin123"), 
             "elian.guevara689@gmail.com", 
             adminRole);
             userRepository.save(defaultAdmin);
             log.info("Usuario administrador por defecto 'admin' creado.");
        }
        
    }
}
