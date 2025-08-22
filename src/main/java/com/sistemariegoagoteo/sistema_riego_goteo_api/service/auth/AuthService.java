package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException; // Específica para credenciales inválidas
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication; // Representa la autenticación
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;

import java.util.Date; // Para actualizar fecha_ultimo_login

/**
 * Servicio para manejar la lógica de autenticación (login) y
 * el registro inicial del usuario Administrador.
 */
@Service
@RequiredArgsConstructor // Lombok: genera constructor con campos final
@Slf4j // Lombok: logger
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager; // Para procesar el login

    /**
     * Autentica a un usuario basado en sus credenciales (username/password).
     * Si la autenticación es exitosa, genera y devuelve un token JWT.
     * Actualiza la fecha del último login.
     *
     * @param authRequest DTO con username y password.
     * @return DTO con el token JWT.
     * @throws BadCredentialsException Si las credenciales son inválidas.
     * @throws RuntimeException        Si el usuario no está activo o no se encuentra (manejado por AuthenticationManager).
     */
    @Transactional // La transacción asegura que la actualización de fecha_ultimo_login se haga junto con la autenticación exitosa
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.info("Intentando autenticar al usuario: {}", authRequest.getUsername());
        try {
            // Intenta autenticar usando el AuthenticationManager configurado en SecurityConfig.
            // Este manager utiliza nuestro JpaUserDetailsService y PasswordEncoder.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            // Si la autenticación llega hasta aquí, fue exitosa.
            // Obtenemos los detalles del usuario autenticado.
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = (User) userDetails; // Hacemos cast a nuestra entidad User

            // Validar si el usuario está activo (aunque UserDetails lo hace, doble check no está mal)
            if (!user.isActive()) {
                log.warn("Intento de login de usuario inactivo: {}", authRequest.getUsername());
                throw new RuntimeException("La cuenta del usuario está inactiva.");
            }

            // Actualizar fecha de último login (opcional pero útil)
            user.setLastLogin(new Date(System.currentTimeMillis())); // Actualiza la fecha de último login
            user.setFailedAttempts(0); // Reiniciar intentos fallidos al loguearse correctamente
            userRepository.save(user); // Guardar los cambios

            // Generar el token JWT
            String jwtToken = jwtService.generateToken(userDetails);
            log.info("Usuario autenticado exitosamente: {}", authRequest.getUsername());

            // --- MODIFICACIÓN ---
            // Se retorna el AuthResponse incluyendo el estado 'active' del usuario.
            // Asegúrate de que el DTO AuthResponse tenga el campo y constructor correspondiente.
            return new AuthResponse(jwtToken, "Bearer", user.isActive());

        } catch (BadCredentialsException e) {
            log.warn("Credenciales inválidas para el usuario: {}", authRequest.getUsername());
            // Opcional: Incrementar contador de intentos fallidos aquí si se maneja el bloqueo
            // handleFailedLoginAttempt(authRequest.getUsername());
            throw new BadCredentialsException("Credenciales inválidas.", e); // Re-lanzar para que el controlador la maneje
        } catch (Exception e) {
            log.error("Error durante la autenticación para {}: {}", authRequest.getUsername(), e.getMessage());
            // Captura otras posibles excepciones (ej. UsernameNotFoundException, DisabledException)
            throw new RuntimeException("Error durante la autenticación: " + e.getMessage(), e);
        }
    }


    /**
     * Registra el *primer* usuario Administrador en el sistema.
     * Este método debería estar protegido o ser llamado de forma controlada (ej. al iniciar la app por primera vez).
     *
     * @param registerRequest DTO con los datos del administrador a registrar.
     * @return El objeto User del administrador creado.
     * @throws RuntimeException Si el rol ADMIN no existe, o si el username/email ya están en uso.
     * @throws IllegalArgumentException Si el rol especificado en el DTO no es "ADMIN".
     */
    @Transactional // Asegura que todas las operaciones de BD se hagan en una transacción
    public User registerAdmin(RegisterRequest registerRequest) {
        log.info("Intentando registrar al usuario ADMIN: {}", registerRequest.getUsername());

        // 1. Validar que el rol solicitado sea ADMIN
        if (!"ADMIN".equalsIgnoreCase(registerRequest.getRol())) {
            throw new IllegalArgumentException("Esta función solo puede registrar usuarios con el rol ADMIN.");
        }

        // 2. Buscar el rol ADMIN en la base de datos
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseThrow(() -> {
                    log.error("El rol ADMIN no existe en la base de datos. Debe ser creado primero.");
                    return new RuntimeException("El rol ADMIN no existe en la base de datos.");
                });

        // 3. Verificar si el username ya existe
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Intento de registro con username existente: {}", registerRequest.getUsername());
            throw new RuntimeException("El nombre de usuario '" + registerRequest.getUsername() + "' ya está en uso.");
        }

        // 4. Verificar si el email ya existe
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Intento de registro con email existente: {}", registerRequest.getEmail());
            throw new RuntimeException("El email '" + registerRequest.getEmail() + "' ya está en uso.");
        }

        // 5. Crear la nueva entidad User
        User newUser = new User();
        newUser.setName(registerRequest.getName());
        newUser.setUsername(registerRequest.getUsername());
        // ¡Importante! Encriptar la contraseña antes de guardarla
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setEmail(registerRequest.getEmail());
        newUser.setRol(adminRole); // Asignar el rol ADMIN encontrado
        newUser.setActive(true); // Por defecto, el admin se crea activo
        newUser.setFailedAttempts(0);
        newUser.setLastLogin(null); // Aún no ha iniciado sesión

        // 6. Guardar el nuevo usuario en la base de datos
        User savedUser = userRepository.save(newUser);
        log.info("Usuario ADMIN registrado exitosamente con ID: {}", savedUser.getId());

        return savedUser;
    }

    // Opcional: Método para manejar intentos fallidos (podría bloquear la cuenta)
    /*
    private void handleFailedLoginAttempt(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getIntentosFallidos() + 1;
            user.setIntentosFallidos(attempts);
            if (attempts >= MAX_LOGIN_ATTEMPTS) { // MAX_LOGIN_ATTEMPTS debería ser una constante
                user.setActivo(false);
                log.warn("Usuario {} bloqueado por exceso de intentos fallidos.", username);
            }
            userRepository.save(user);
        });
    }
    */

}