package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UserService.
 * Cubre las operaciones CRUD de usuarios realizadas por el administrador
 * y las operaciones de self-service del propio usuario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Tests Unitarios")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User targetUser;
    private Role adminRole;
    private Role operarioRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role("ADMIN");
        operarioRole = new Role("OPERARIO");

        adminUser = new User("Admin", "admin", "encoded_pass", "admin@test.com", adminRole);
        adminUser.setId(1L);
        adminUser.setActive(true);

        targetUser = new User("Operario Test", "operario_test", "encoded_pass2", "operario@test.com", operarioRole);
        targetUser.setId(2L);
        targetUser.setActive(true);

        // Simular usuario autenticado en el SecurityContext
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, new java.util.ArrayList<>()));
    }

    // =======================================================================
    // registerUserByAdmin
    // =======================================================================

    @Test
    @DisplayName("registerUserByAdmin() debe guardar usuario y notificar al crear exitosamente")
    void registerUserByAdmin_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Nuevo Operario");
        request.setUsername("nuevo_operario");
        request.setPassword("pass123");
        request.setEmail("nuevo@test.com");
        request.setRol("OPERARIO");

        when(roleRepository.findByRoleName("OPERARIO")).thenReturn(Optional.of(operarioRole));
        when(userRepository.existsByUsername("nuevo_operario")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded_pass3");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(3L);
            return u;
        });

        User result = userService.registerUserByAdmin(request);

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("nuevo_operario", result.getUsername());
        verify(userRepository).save(any(User.class));
        // Verificar que se enviaron las dos notificaciones (admin + nuevo usuario)
        verify(notificationService, times(2)).createNotification(any(User.class), anyString(), anyString(), any(),
                anyString());
    }

    @Test
    @DisplayName("registerUserByAdmin() debe lanzar excepción con un rol inválido")
    void registerUserByAdmin_InvalidRole_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setRol("SUPERUSUARIO"); // Rol inválido

        assertThrows(IllegalArgumentException.class, () -> userService.registerUserByAdmin(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUserByAdmin() debe lanzar excepción si el username ya existe")
    void registerUserByAdmin_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin");
        request.setRol("OPERARIO");
        request.setEmail("otro@test.com");

        when(roleRepository.findByRoleName("OPERARIO")).thenReturn(Optional.of(operarioRole));
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.registerUserByAdmin(request));
        verify(userRepository, never()).save(any());
    }

    // =======================================================================
    // assignUserToFarm
    // =======================================================================

    @Test
    @DisplayName("assignUserToFarm() debe asignar exitosamente si es OPERARIO y no tiene fincas")
    void assignUserToFarm_Success() {
        com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm farm = new com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm();
        farm.setId(1);
        farm.setName("Finca Test");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        userService.assignUserToFarm(2L, 1);

        assertTrue(targetUser.getFarms().contains(farm));
        verify(userRepository).save(targetUser);
        verify(auditService).logChange(any(User.class), eq("ASSIGN"), eq("user_farm"), eq("farm_id"), isNull(),
                eq("1"));
        verify(notificationService).createNotification(eq(targetUser), anyString(), eq("FARM"), eq(1L), anyString());
    }

    @Test
    @DisplayName("assignUserToFarm() debe lanzar excepción si el usuario NO es OPERARIO")
    void assignUserToFarm_NotOperario_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser)); // adminUser tiene rol ADMIN

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.assignUserToFarm(1L, 1));

        assertEquals("Solo los usuarios con rol OPERARIO pueden ser asignados a una finca.", exception.getMessage());
        verify(farmRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("assignUserToFarm() debe lanzar excepción si el OPERARIO ya está en otra finca")
    void assignUserToFarm_AlreadyAssigned_ThrowsException() {
        com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm existingFarm = new com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm();
        existingFarm.setId(2);
        targetUser.getFarms().add(existingFarm);

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.assignUserToFarm(2L, 1));

        assertEquals("El operario ya está asignado a otra finca. Debe desasignarlo primero.", exception.getMessage());
        verify(farmRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    // =======================================================================
    // updateUserStatus
    // =======================================================================

    @Test
    @DisplayName("updateUserStatus() debe desactivar usuario y enviar notificación")
    void updateUserStatus_Deactivate_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUserStatus(2L, false);

        assertFalse(result.isActive());
        verify(notificationService).createNotification(eq(targetUser), anyString(), eq("GENERAL"), isNull(),
                anyString());
    }

    @Test
    @DisplayName("updateUserStatus() no debe hacer nada si el estado ya es el mismo")
    void updateUserStatus_SameState_NoAudit() {
        // targetUser ya está activo (true), y pedimos active=true
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.updateUserStatus(2L, true);

        // No debe llamar a auditService porque el estado no cambió
        verify(auditService, never()).logChange(any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // =======================================================================
    // deleteUser
    // =======================================================================

    @Test
    @DisplayName("deleteUser() debe eliminar el usuario y registrar auditoría")
    void deleteUser_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        userService.deleteUser(2L);

        verify(userRepository).delete(targetUser);
        verify(auditService).logChange(any(User.class), eq("DELETE"), eq("User"), eq("id"), eq("2"), isNull());
    }

    @Test
    @DisplayName("deleteUser() debe lanzar ResourceNotFoundException si el usuario no existe")
    void deleteUser_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).delete(any());
    }

    // =======================================================================
    // updateOwnPassword
    // =======================================================================

    @Test
    @DisplayName("updateOwnPassword() debe cambiar contraseña si la actual es correcta")
    void updateOwnPassword_Success() {
        when(userRepository.findByUsername("operario_test")).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.matches("vieja_pass", targetUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("nueva_pass")).thenReturn("encoded_nueva");

        userService.updateOwnPassword("operario_test", "vieja_pass", "nueva_pass", "nueva_pass");

        verify(userRepository).save(targetUser);
        verify(notificationService).createNotification(eq(targetUser), anyString(), eq("GENERAL"), isNull(),
                anyString());
    }

    @Test
    @DisplayName("updateOwnPassword() debe lanzar excepción si las contraseñas no coinciden")
    void updateOwnPassword_PasswordMismatch_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateOwnPassword("operario_test", "vieja_pass", "nueva1", "nueva2"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOwnPassword() debe lanzar BadCredentialsException si la contraseña actual es incorrecta")
    void updateOwnPassword_WrongCurrentPassword_ThrowsException() {
        when(userRepository.findByUsername("operario_test")).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.matches("pass_incorrecta", targetUser.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> userService.updateOwnPassword("operario_test", "pass_incorrecta", "nueva", "nueva"));
        verify(userRepository, never()).save(any());
    }
}
