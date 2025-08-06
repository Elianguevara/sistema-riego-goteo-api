package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

// Imports necesarios
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.UserStatsResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService; // <-- IMPORTAR

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder; // <-- IMPORTAR
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects; // <-- IMPORTAR
import java.util.stream.Collectors;

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
    private final AuditService auditService; // <-- INYECTAR SERVICIO DE AUDITORÍA

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User registerUserByAdmin(RegisterRequest registerRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String requestedRoleName = registerRequest.getRol().toUpperCase();
        log.info("Admin {} intentando registrar usuario: {} con rol: {}", currentUser.getUsername(), registerRequest.getUsername(), requestedRoleName);

        List<String> rolesPermitidos = List.of("ADMIN", "ANALISTA", "OPERARIO");
        if (!rolesPermitidos.contains(requestedRoleName)) {
            throw new IllegalArgumentException("El rol especificado no es válido. Roles permitidos: ADMIN, ANALISTA, OPERARIO.");
        }

        Role targetRole = roleRepository.findByRoleName(requestedRoleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "nombreRol", requestedRoleName));

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("El nombre de usuario '" + registerRequest.getUsername() + "' ya está en uso.");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
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
        
        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", User.class.getSimpleName(), "all", null, "Nuevo usuario ID: " + savedUser.getId());

        return savedUser;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(Long id, UserUpdateRequest updateRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Admin {} intentando actualizar usuario con ID: {}", currentUser.getUsername(), id);
        User user = findUserById(id);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(user.getName(), updateRequest.getName())) {
            auditService.logChange(currentUser, "UPDATE", User.class.getSimpleName(), "name", user.getName(), updateRequest.getName());
        }
        if (!user.getEmail().equalsIgnoreCase(updateRequest.getEmail())) {
            if (userRepository.existsByEmail(updateRequest.getEmail())) {
                throw new RuntimeException("El email '" + updateRequest.getEmail() + "' ya está en uso por otro usuario.");
            }
            auditService.logChange(currentUser, "UPDATE", User.class.getSimpleName(), "email", user.getEmail(), updateRequest.getEmail());
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
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Admin {} intentando cambiar estado activo a {} para usuario con ID: {}", currentUser.getUsername(), status, id);
        User user = findUserById(id);

        // --- AUDITORÍA DE CAMBIO DE ESTADO ---
        if (user.isActive() != status) {
            auditService.logChange(currentUser, "UPDATE", User.class.getSimpleName(), "isActive", String.valueOf(user.isActive()), String.valueOf(status));
        }

        user.setActive(status);
        User updatedUser = userRepository.save(user);
        log.info("Estado activo del usuario con ID: {} cambiado a {} exitosamente por admin.", id, status);
        return updatedUser;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.warn("Admin {} intentando ELIMINAR PERMANENTEMENTE usuario con ID: {}", currentUser.getUsername(), id);
        User user = findUserById(id);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", User.class.getSimpleName(), "id", user.getId().toString(), null);

        userRepository.delete(user);
        log.info("Usuario con ID: {} eliminado permanentemente por admin.", id);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updatePassword(Long userId, String rawPassword) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Admin {} intentando actualizar contraseña para usuario con ID: {}", currentUser.getUsername(), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // --- AUDITORÍA DE CAMBIO DE CONTRASEÑA (ADMIN) ---
        auditService.logChange(currentUser, "UPDATE", User.class.getSimpleName(), "password", "********", "********");

        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFailedAttempts(0);
        userRepository.save(user);
        log.info("Contraseña del usuario con ID: {} actualizada exitosamente por admin.", userId);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void assignUserToFarm(Long userId, Integer farmId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Admin {} intentando asignar usuario ID {} a finca ID {}", currentUser.getUsername(), userId, farmId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        // --- AUDITORÍA DE ASIGNACIÓN ---
        auditService.logChange(currentUser, "ASSIGN", "user_farm", "farm_id", null, farmId.toString());

        user.getFarms().add(farm);
        userRepository.save(user);
        log.info("Usuario ID {} asignado exitosamente a la finca ID {}", userId, farmId);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void unassignUserFromFarm(Long userId, Integer farmId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Admin {} intentando desasignar usuario ID {} de la finca ID {}", currentUser.getUsername(), userId, farmId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        if (user.getFarms().remove(farm)) {
            // --- AUDITORÍA DE DESASIGNACIÓN ---
            auditService.logChange(currentUser, "UNASSIGN", "user_farm", "farm_id", farmId.toString(), null);
            userRepository.save(user);
            log.info("Usuario ID {} desasignado exitosamente de la finca ID {}", userId, farmId);
        } else {
            log.warn("El usuario ID {} no estaba asignado a la finca ID {}, no se realizó ninguna acción.", userId, farmId);
        }
    }

    // --- MÉTODOS SIN CAMBIOS DE AUDITORÍA ---
    
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
    public void updateOwnPassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        log.info("Usuario {} intentando actualizar su propia contraseña.", username);

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("La nueva contraseña y su confirmación no coinciden.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("La contraseña actual es incorrecta.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Contraseña del usuario {} actualizada exitosamente.", username);
    }

    @Transactional
    public User updateOwnProfile(String username, UserUpdateRequest request) {
        log.info("Usuario {} intentando actualizar su perfil.", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email '" + request.getEmail() + "' ya está en uso por otro usuario.");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findUsersByFarm(Integer farmId) {
        log.debug("Admin buscando usuarios para la finca ID: {}", farmId);
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", "id", farmId);
        }
        return userRepository.findByFarms_Id(farmId);
    }
    // --- MÉTODO PARA EL DASHBOARD ---

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public UserStatsResponse getUserStats() {
        log.info("Generando estadísticas de usuarios para el dashboard de administrador.");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActive(true);
        long inactiveUsers = totalUsers - activeUsers;

        Map<String, Long> usersByRole = userRepository.countUsersByRole().stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0], // Nombre del rol
                    row -> (Long) row[1]    // Conteo
                ));

        return new UserStatsResponse(totalUsers, activeUsers, inactiveUsers, usersByRole);
    }
}