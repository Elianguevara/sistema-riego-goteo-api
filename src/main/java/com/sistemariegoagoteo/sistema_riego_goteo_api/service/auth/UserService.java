package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

// Imports necesarios
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm; // <<-- AÑADIR IMPORT
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository; // <<-- AÑADIR IMPORT

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final FarmRepository farmRepository;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User registerUserByAdmin(RegisterRequest registerRequest) {
        String requestedRoleName = registerRequest.getRol().toUpperCase();
        log.info("Admin intentando registrar usuario: {} con rol: {}", registerRequest.getUsername(), requestedRoleName);

        // --- INICIO DE LA MODIFICACIÓN ---
        // Se reemplaza la validación anterior por una que incluye a ADMIN.
        List<String> rolesPermitidos = List.of("ADMIN", "ANALISTA", "OPERARIO");
        if (!rolesPermitidos.contains(requestedRoleName)) {
            throw new IllegalArgumentException("El rol especificado no es válido. Roles permitidos: ADMIN, ANALISTA, OPERARIO.");
        }
        // --- FIN DE LA MODIFICACIÓN ---

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
    @Transactional
    public void updateOwnPassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        log.info("Usuario {} intentando actualizar su propia contraseña.", username);

        // 1. Validar que la nueva contraseña y su confirmación coincidan
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("La nueva contraseña y su confirmación no coinciden.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // 2. Verificar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Intento de cambio de contraseña fallido para el usuario {}: contraseña actual incorrecta.", username);
            throw new BadCredentialsException("La contraseña actual es incorrecta.");
        }

        // 3. Si todo es correcto, actualizar la contraseña
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Contraseña del usuario {} actualizada exitosamente.", username);
    }
    @Transactional
    public User updateOwnProfile(String username, UserUpdateRequest request) {
        log.info("Usuario {} intentando actualizar su perfil.", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Validar si el nuevo email ya está en uso por OTRO usuario
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email '" + request.getEmail() + "' ya está en uso por otro usuario.");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        return userRepository.save(user);
    }
    /**
     * Asigna un usuario a una finca.
     *
     * @param userId El ID del usuario a asignar.
     * @param farmId El ID de la finca a la que se asignará.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void assignUserToFarm(Long userId, Integer farmId) {
        log.info("Admin intentando asignar usuario ID {} a finca ID {}", userId, farmId);

        // 1. Encontrar el usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 2. Encontrar la finca
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        // 3. Añadir la finca al conjunto de fincas del usuario
        user.getFarms().add(farm);

        // 4. Guardar el usuario. JPA se encargará de actualizar la tabla de unión.
        userRepository.save(user);
        log.info("Usuario ID {} asignado exitosamente a la finca ID {}", userId, farmId);
    }

    /**
     * Desasigna un usuario de una finca.
     *
     * @param userId El ID del usuario a desasignar.
     * @param farmId El ID de la finca de la que se desasignará.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void unassignUserFromFarm(Long userId, Integer farmId) {
        log.info("Admin intentando desasignar usuario ID {} de la finca ID {}", userId, farmId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        // Remover la finca del conjunto de fincas del usuario
        if (user.getFarms().remove(farm)) {
            userRepository.save(user);
            log.info("Usuario ID {} desasignado exitosamente de la finca ID {}", userId, farmId);
        } else {
            log.warn("El usuario ID {} no estaba asignado a la finca ID {}, no se realizó ninguna acción.", userId, farmId);
            // Opcional: podrías lanzar una excepción si lo consideras un error
        }
    }

    /**
     * Busca y devuelve todos los usuarios asignados a una finca específica.
     *
     * @param farmId El ID de la finca.
     * @return Una lista de entidades User.
     * @throws ResourceNotFoundException si la finca no existe.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findUsersByFarm(Integer farmId) {
        log.debug("Admin buscando usuarios para la finca ID: {}", farmId);

        // 1. Validar que la finca exista para dar un error claro
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", "id", farmId);
        }

        // 2. Usar el nuevo método del repositorio para obtener los usuarios
        return userRepository.findByFarms_Id(farmId);
    }
}
