package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.UserStatsResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user.UserUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private Role adminRole;
    private Role operatorRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role(1, "ADMIN", new java.util.HashSet<>());
        operatorRole = new Role(3, "OPERARIO", new java.util.HashSet<>());

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRol(adminRole);

        // Configurar el SecurityContext con el admin mockeado
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null,
                        new ArrayList<org.springframework.security.core.GrantedAuthority>()));
    }

    @Test
    void registerUserByAdmin_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("nuevoUser");
        request.setEmail("nuevo@test.com");
        request.setPassword("pass123");
        request.setName("Nuevo Usuario");
        request.setRol("OPERARIO");

        when(roleRepository.findByRoleName("OPERARIO")).thenReturn(Optional.of(operatorRole));
        when(userRepository.existsByUsername("nuevoUser")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPass");

        User savedMockUser = new User();
        savedMockUser.setId(2L);
        savedMockUser.setUsername("nuevoUser");
        savedMockUser.setRol(operatorRole);
        when(userRepository.save(any(User.class))).thenReturn(savedMockUser);

        // Act
        User result = userService.registerUserByAdmin(request);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        verify(userRepository).save(any(User.class));
        verify(auditService).logChange(eq(adminUser), eq("CREATE"), eq("User"), eq("all"), isNull(),
                contains("Nuevo usuario ID: 2"));
        verify(notificationService, times(2)).createNotification(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void registerUserByAdmin_InvalidRole_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setRol("INVALID_ROLE");

        assertThrows(IllegalArgumentException.class, () -> userService.registerUserByAdmin(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserByAdmin_UsernameExists_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin");
        request.setRol("OPERARIO");

        when(roleRepository.findByRoleName("OPERARIO")).thenReturn(Optional.of(operatorRole));
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.registerUserByAdmin(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_Success() {
        Long userId = 2L;
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Nuevo Nombre");
        request.setEmail("nuevo@email.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setName("Viejo Nombre");
        existingUser.setEmail("viejo@email.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("nuevo@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User result = userService.updateUser(userId, request);

        assertEquals("Nuevo Nombre", result.getName());
        assertEquals("nuevo@email.com", result.getEmail());
        verify(auditService, times(2)).logChange(any(), eq("UPDATE"), any(), anyString(), anyString(), anyString());
    }

    @Test
    void updateUserStatus_Success() {
        Long userId = 2L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User result = userService.updateUserStatus(userId, false);

        assertFalse(result.isActive());
        verify(auditService).logChange(any(), eq("UPDATE"), any(), eq("isActive"), eq("true"), eq("false"));
        verify(notificationService).createNotification(eq(existingUser), anyString(), eq("GENERAL"), isNull(),
                anyString());
    }

    @Test
    void deleteUser_Success() {
        Long userId = 2L;
        User existingUser = new User();
        existingUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        userService.deleteUser(userId);

        verify(userRepository).delete(existingUser);
        verify(auditService).logChange(any(), eq("DELETE"), eq("User"), eq("id"), eq("2"), isNull());
    }

    @Test
    void assignUserToFarm_Success() {
        Long userId = 2L;
        Integer farmId = 1;

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setFarms(new java.util.HashSet<Farm>());

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setName("Finca Test");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(farm));

        userService.assignUserToFarm(userId, farmId);

        assertTrue(existingUser.getFarms().contains(farm));
        verify(userRepository).save(existingUser);
        verify(auditService).logChange(any(), eq("ASSIGN"), eq("user_farm"), eq("farm_id"), isNull(), eq("1"));
        verify(notificationService).createNotification(eq(existingUser), anyString(), eq("FARM"), eq(1L), anyString());
    }

    @Test
    void updateOwnPassword_Success() {
        String username = "testuser";
        User user = new User();
        user.setUsername(username);
        user.setPassword("oldHash");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");

        userService.updateOwnPassword(username, "oldPass", "newPass", "newPass");

        assertEquals("newHash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void updateOwnPassword_PasswordsNotMatch_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateOwnPassword("username", "oldPass", "newPass", "wrongPass"));
    }

    @Test
    void getUserStats_Success() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByIsActive(true)).thenReturn(8L);

        List<Object[]> roleStats = new ArrayList<>();
        roleStats.add(new Object[] { "ADMIN", 2L });
        roleStats.add(new Object[] { "OPERARIO", 8L });
        when(userRepository.countUsersByRole()).thenReturn(roleStats);

        UserStatsResponse response = userService.getUserStats();

        assertEquals(10L, response.getTotalUsers());
        assertEquals(8L, response.getActiveUsers());
        assertEquals(2L, response.getInactiveUsers());
        assertEquals(2L, response.getUsersByRole().get("ADMIN"));
    }
}
