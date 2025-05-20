package com.sistemariegoagoteo.sistema_riego_goteo_api.model.user;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm; // Asegúrate de importar la entidad Farm
import jakarta.persistence.*;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
//import java.util.stream.Collectors; // Para la opción de autoridades con permisos

/**
 * Entidad que representa un Usuario en el sistema.
 * Mapea a la tabla 'user' del MER y implementa UserDetails para Spring Security.
 */
@Setter @Getter
@NoArgsConstructor
@Entity
@Table(name = "user") // La tabla en el MER se llama 'user'
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id") // Mapea a la columna 'user_id' en la BD
    private Long id;

    @Column(name = "name", nullable = false, length = 100) // Columna 'name' en el MER
    private String name; // Cambiado de 'nombre'

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "last_login") // Columna 'last_login' en el MER
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin; // Cambiado de 'fechaUltimoLogin'

    @Column(name = "failed_attempts", columnDefinition = "INT DEFAULT 0") // Columna 'failed_attempts' en el MER
    private Integer failedAttempts = 0; // Cambiado de 'intentosFallidos'

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false) // Columna 'role_id' en el MER (FK a la tabla 'role')
    private Role rol;

    @Column(name = "is_active", columnDefinition = "TINYINT(1) DEFAULT 1") // Columna 'is_active' en el MER
    private boolean isActive = true; // Cambiado de 'activo'

    /**
     * Relación Many-to-Many con la entidad Farm, a través de la tabla intermedia 'user_farm'.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_farm", // Tabla de unión según el MER
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "farm_id")
    )
    @ToString.Exclude // Evitar recursión en toString con Lombok
    @EqualsAndHashCode.Exclude // Evitar recursión en equals/hashCode con Lombok
    private Set<Farm> farms = new HashSet<>();

    // Constructor conveniente actualizado
    public User(String name, String username, String password, String email, Role rol) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.rol = rol;
        this.isActive = true;
        this.failedAttempts = 0;
    }

    // --- Implementación de UserDetails ---

    /**
     * Retorna las autoridades concedidas al usuario.
     * Por defecto, se basa en el rol principal del usuario.
     * Si se implementa un sistema de permisos más granular (Role <-> Permission),
     * este método podría expandirse para incluir los permisos directamente como autoridades.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Opción 1: Solo basado en el Rol (como estaba)
        if (this.rol == null) {
            return List.of();
        }
        SimpleGrantedAuthority roleAuthority = new SimpleGrantedAuthority("ROLE_" + this.rol.getRoleName());
        return List.of(roleAuthority);

        // Opción 2: Basado en Rol y sus Permisos asociados (si Role tiene una colección de Permission)
        // Asegúrate de que Role.java tenga la relación @ManyToMany con Permission y que los permisos se carguen (EAGER o Fetch).
        /*
        if (this.rol == null) {
            return List.of();
        }
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.rol.getNombreRol()));
        if (this.rol.getPermissions() != null) {
            this.rol.getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.getPermissionName()))
            );
        }
        return authorities;
        */
    }

    // getPassword() y getUsername() son generados por Lombok (@Data)

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica si la cuenta del usuario no está bloqueada.
     * Ahora usa el campo 'isActive' y podría combinarse con 'failedAttempts'.
     */
    @Override
    public boolean isAccountNonLocked() {
        // Ejemplo más avanzado: return isActive && failedAttempts < MAX_LOGIN_ATTEMPTS;
        return this.isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica si el usuario está habilitado o deshabilitado.
     * Mapea directamente al campo 'isActive'.
     */
    @Override
    public boolean isEnabled() {
        return this.isActive;
    }
}