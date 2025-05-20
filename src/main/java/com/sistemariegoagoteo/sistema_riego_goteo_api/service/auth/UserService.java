package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

// Imports necesarios
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
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

/**
 * Servicio para gestionar operaciones de usuario realizadas por un Administrador.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User registerUserByAdmin(RegisterRequest registerRequest) {
        String requestedRoleName = registerRequest.getRol().toUpperCase();
        log.info("Admin intentando registrar usuario: {} con rol: {}", registerRequest.getUsername(), requestedRoleName);

        if (!"ANALISTA".equals(requestedRoleName) && !"OPERARIO".equals(requestedRoleName)) {
            throw new IllegalArgumentException("Solo se pueden registrar usuarios con los roles ANALISTA u OPERARIO a través de esta función.");
        }
        Role targetRole = roleRepository.findByRoleName(requestedRoleName)
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
        newUser.setName(registerRequest.getName());
        newUser.setUsername(registerRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setEmail(registerRequest.getEmail());
        newUser.setRol(targetRole);
        newUser.setActive(true);
        newUser.setFailedAttempts(0);
        newUser.setLastLogin(null);

        User savedUser = userRepository.save(newUser);
        log.info("Usuario {} registrado exitosamente por admin con ID: {}", requestedRoleName, savedUser.getId());
        return savedUser;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findAllUsers() {
        log.debug("Admin solicitando lista de todos los usuarios");
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public User findUserById(Long id) {
        log.debug("Admin buscando usuario con ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findUsersByRole(String roleName) {
        log.debug("Admin buscando usuarios con rol: {}", roleName);
        Role role = roleRepository.findByRoleName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "nombreRol", roleName));
        return userRepository.findByRol(role);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(Long id, UserUpdateRequest updateRequest) {
        log.info("Admin intentando actualizar usuario con ID: {}", id);
        User user = findUserById(id);

        if (!user.getEmail().equalsIgnoreCase(updateRequest.getEmail()) &&
            userRepository.existsByEmail(updateRequest.getEmail())) {
            log.warn("Intento de actualizar usuario {} con email existente: {}", id, updateRequest.getEmail());
            throw new RuntimeException("El email '" + updateRequest.getEmail() + "' ya está en uso por otro usuario.");
        }

        user.setName(updateRequest.getName());
        user.setEmail(updateRequest.getEmail());

        User updatedUser = userRepository.save(user);
        log.info("Usuario con ID: {} actualizado exitosamente por admin.", id);
        return updatedUser;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUserStatus(Long id, boolean status) {
        log.info("Admin intentando cambiar estado activo a {} para usuario con ID: {}", status, id);
        User user = findUserById(id);
        user.setActive(status);
        User updatedUser = userRepository.save(user);
        log.info("Estado activo del usuario con ID: {} cambiado a {} exitosamente por admin.", id, status);
        return updatedUser;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        log.warn("Admin intentando ELIMINAR PERMANENTEMENTE usuario con ID: {}", id);
        User user = findUserById(id);
        userRepository.delete(user);
        log.info("Usuario con ID: {} eliminado permanentemente por admin.", id);
    }

    /**
     * Actualiza la contraseña de un usuario existente. Solo accesible por ADMIN.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updatePassword(Long userId, String rawPassword) {
        log.info("Admin intentando actualizar contraseña para usuario con ID: {}", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFailedAttempts(0);
        userRepository.save(user);
        log.info("Contraseña del usuario con ID: {} actualizada exitosamente por admin.", userId);
    }
}
