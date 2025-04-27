package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

// Imports corregidos y añadidos
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest; // Import corregido
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
// Optional ya no es necesario para el valor de retorno de findUserById
// import java.util.Optional;

/**
 * Servicio para gestionar operaciones de usuario realizadas por un Administrador.
 * Incluye registro, búsqueda, listado, actualización, activación/desactivación y eliminación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario (Analista u Operario) por un Administrador.
     * (Método existente - sin cambios)
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User registerUserByAdmin(RegisterRequest registerRequest) {
        String requestedRoleName = registerRequest.getRol().toUpperCase();
        log.info("Admin intentando registrar usuario: {} con rol: {}", registerRequest.getUsername(), requestedRoleName);

        if (!"ANALISTA".equals(requestedRoleName) && !"OPERARIO".equals(requestedRoleName)) {
            throw new IllegalArgumentException("Solo se pueden registrar usuarios con los roles ANALISTA u OPERARIO a través de esta función.");
        }
        Role targetRole = roleRepository.findByNombreRol(requestedRoleName)
                 // Usar ResourceNotFoundException aquí también para consistencia
                .orElseThrow(() -> new ResourceNotFoundException("Role", "nombreRol", requestedRoleName));

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
             log.warn("Intento de registro (admin) con username existente: {}", registerRequest.getUsername());
            throw new RuntimeException("El nombre de usuario '" + registerRequest.getUsername() + "' ya está en uso.");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Intento de registro (admin) con email existente: {}", registerRequest.getEmail());
            throw new RuntimeException("El email '" + registerRequest.getEmail() + "' ya está en uso.");
        }

        User newUser = new User();
        newUser.setNombre(registerRequest.getNombre());
        newUser.setUsername(registerRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setEmail(registerRequest.getEmail());
        newUser.setRol(targetRole);
        newUser.setActivo(true);
        newUser.setIntentosFallidos(0);
        newUser.setFechaUltimoLogin(null);

        User savedUser = userRepository.save(newUser);
        log.info("Usuario {} registrado exitosamente por admin con ID: {}", requestedRoleName, savedUser.getId());
        return savedUser;
    }

    // --- Métodos para Gestión de Usuarios por Admin (Corregidos/Restaurados) ---

    /**
     * Busca y devuelve todos los usuarios del sistema.
     * @return Lista de todos los usuarios.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findAllUsers() {
        log.debug("Admin solicitando lista de todos los usuarios");
        return userRepository.findAll();
    }

    /**
     * Busca un usuario por su ID. Lanza excepción si no lo encuentra.
     * @param id El ID del usuario a buscar.
     * @return El usuario encontrado.
     * @throws ResourceNotFoundException si el usuario no existe.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public User findUserById(Long id) { // Devuelve User directamente
        log.debug("Admin buscando usuario con ID: {}", id);
        return userRepository.findById(id)
                // Lanza la excepción aquí si no se encuentra
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

     /**
     * Busca y devuelve todos los usuarios que pertenecen a un rol específico.
     * @param roleName Nombre del rol (ej. "ANALISTA", "OPERARIO"). Case-insensitive.
     * @return Lista de usuarios con ese rol.
     * @throws ResourceNotFoundException si el rol especificado no existe.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findUsersByRole(String roleName) { // Método restaurado
        log.debug("Admin buscando usuarios con rol: {}", roleName);
        Role role = roleRepository.findByNombreRol(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "nombreRol", roleName));
        // Usa el método añadido al UserRepository
        return userRepository.findByRol(role);
    }


    /**
     * Actualiza los datos (nombre, email) de un usuario existente.
     * @param id El ID del usuario a actualizar.
     * @param updateRequest DTO con los nuevos datos.
     * @return El usuario actualizado.
     * @throws ResourceNotFoundException si el usuario no existe.
     * @throws RuntimeException si el nuevo email ya está en uso por otro usuario.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(Long id, UserUpdateRequest updateRequest) {
        log.info("Admin intentando actualizar usuario con ID: {}", id);
        // findUserById ahora lanza la excepción si no existe
        User user = findUserById(id);

        // Verificar si el email ha cambiado y si el nuevo email ya está en uso por OTRO usuario
        if (!user.getEmail().equalsIgnoreCase(updateRequest.getEmail()) &&
            userRepository.existsByEmail(updateRequest.getEmail())) {
             log.warn("Intento de actualizar usuario {} con email existente: {}", id, updateRequest.getEmail());
            throw new RuntimeException("El email '" + updateRequest.getEmail() + "' ya está en uso por otro usuario.");
        }

        user.setNombre(updateRequest.getNombre());
        user.setEmail(updateRequest.getEmail());

        User updatedUser = userRepository.save(user);
        log.info("Usuario con ID: {} actualizado exitosamente por admin.", id);
        return updatedUser;
    }

    /**
     * Cambia el estado de activación (habilita/deshabilita) de un usuario.
     * @param id El ID del usuario a modificar.
     * @param status true para activar, false para desactivar.
     * @return El usuario con el estado actualizado.
     * @throws ResourceNotFoundException si el usuario no existe.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUserStatus(Long id, boolean status) {
        log.info("Admin intentando cambiar estado activo a {} para usuario con ID: {}", status, id);
         // findUserById ahora lanza la excepción si no existe
        User user = findUserById(id);
        user.setActivo(status);
        User updatedUser = userRepository.save(user);
         log.info("Estado activo del usuario con ID: {} cambiado a {} exitosamente por admin.", id, status);
        return updatedUser;
    }

    /**
     * Elimina permanentemente un usuario de la base de datos.
     * @param id El ID del usuario a eliminar.
     * @throws ResourceNotFoundException si el usuario no existe.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        log.warn("Admin intentando ELIMINAR PERMANENTEMENTE usuario con ID: {}", id);
         // findUserById ahora lanza la excepción si no existe, por lo que no necesitamos existsById aquí
        User user = findUserById(id);
        userRepository.delete(user); // O deleteById(id) también funciona
        log.info("Usuario con ID: {} eliminado permanentemente por admin.", id);
    }
}
