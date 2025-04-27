package com.sistemariegoagoteo.sistema_riego_goteo_api.model.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Entidad que representa un Usuario en el sistema.
 * Mapea a la tabla 'usuario' y implementa UserDetails para Spring Security.
 */
@Data // Genera getters, setters, toString, equals, hashCode
@NoArgsConstructor // Genera constructor sin argumentos
@Entity
@Table(name = "usuario")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario") // Mapea a la columna 'id_usuario' en la BD
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username; // Usado para el login

    @Column(name = "password", nullable = false, length = 255)
    private String password; // Contraseña ENCRIPTADA

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "fecha_ultimo_login")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaUltimoLogin;

    @Column(name = "intentos_fallidos", columnDefinition = "INT DEFAULT 0")
    private Integer intentosFallidos = 0;

    /**
     * Relación Many-to-One con la entidad Role.
     * FetchType.EAGER asegura que el rol se cargue junto con el usuario.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false) // Columna FK en la tabla 'usuario'
    private Role rol;

    @Column(name = "activo", columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean activo = true; // Indica si el usuario está activo

    // Constructor conveniente para crear usuarios
    public User(String nombre, String username, String password, String email, Role rol) {
        this.nombre = nombre;
        this.username = username;
        this.password = password; // ¡Asegúrate de encriptar antes de guardar!
        this.email = email;
        this.rol = rol;
        this.activo = true;
        this.intentosFallidos = 0;
    }

    // --- Implementación de UserDetails ---

    /**
     * Retorna las autoridades (roles) concedidas al usuario.
     * Spring Security usa esto para la autorización. El prefijo "ROLE_" es importante.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Devuelve una lista que contiene una única autoridad basada en el nombre del rol.
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.getNombreRol()));
    }

    // getPassword() y getUsername() son generados por Lombok (@Data)

    /**
     * Indica si la cuenta del usuario no ha expirado.
     * Para este ejemplo, siempre retorna true. Puedes añadir lógica si es necesario.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // Puedes implementar lógica de expiración si la necesitas
    }

    /**
     * Indica si la cuenta del usuario no está bloqueada.
     * Podrías usar el campo 'intentosFallidos' para implementar bloqueo.
     * Por ahora, depende directamente del estado 'activo'.
     */
    @Override
    public boolean isAccountNonLocked() {
         // Podrías añadir lógica aquí, por ejemplo: return activo && intentosFallidos < MAX_ATTEMPTS;
         return activo;
    }

    /**
     * Indica si las credenciales del usuario (contraseña) no han expirado.
     * Para este ejemplo, siempre retorna true.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Puedes implementar lógica de expiración de contraseña
    }

    /**
     * Indica si el usuario está habilitado o deshabilitado.
     * Mapea directamente al campo 'activo'.
     */
    @Override
    public boolean isEnabled() {
        return this.activo;
    }
}