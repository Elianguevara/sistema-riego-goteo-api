package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;



import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importante para operaciones de lectura

import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;

/**
 * Implementación personalizada de UserDetailsService.
 * Carga los detalles del usuario desde la base de datos usando UserRepository.
 * Spring Security utiliza este servicio durante el proceso de autenticación.
 */
@Service
@RequiredArgsConstructor // Lombok: genera un constructor con los campos final requeridos (userRepository)
public class JpaUserDetailsService implements UserDetailsService {

    // Inyección de dependencia del UserRepository (final para asegurar inicialización)
    private final UserRepository userRepository;

    /**
     * Carga un usuario por su nombre de usuario.
     * Este método es invocado por Spring Security al autenticar.
     *
     * @param username El nombre de usuario (username) proporcionado durante el login.
     * @return Un objeto UserDetails que contiene la información del usuario (incluyendo contraseña y roles).
     * @throws UsernameNotFoundException Si no se encuentra ningún usuario con ese username.
     */
    @Override
    @Transactional(readOnly = true) // Optimiza la transacción para lectura
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Busca el usuario en la base de datos a través del repositorio
        return userRepository.findByUsername(username)
                // Si no se encuentra, lanza la excepción estándar de Spring Security
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username: " + username));

        // Nota: La entidad User ya implementa UserDetails, por lo que podemos devolverla directamente.
        // Spring Security utilizará los métodos de UserDetails (getPassword, getAuthorities, etc.)
        // implementados en la entidad User.
    }
}
