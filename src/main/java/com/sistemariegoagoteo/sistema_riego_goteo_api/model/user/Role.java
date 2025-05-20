package com.sistemariegoagoteo.sistema_riego_goteo_api.model.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString; // Necesario para @ToString.Exclude
import lombok.EqualsAndHashCode; // Necesario para @EqualsAndHashCode.Exclude

import java.util.HashSet; // Para inicializar el Set
import java.util.Set;

/**
 * Entidad que representa un Rol en el sistema.
 * Mapea a la tabla 'role' del MER.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role") // La tabla en el MER se llama 'role'
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id") // Mapea a la columna 'role_id' en la BD según el MER
    private Integer id;

    /**
     * Nombre único del rol (ej. "ADMIN", "ANALISTA", "OPERARIO").
     * Se usa para la lógica de autorización.
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 50) // Columna 'role_name' en el MER
    private String roleName; // Cambiado de 'nombreRol' para consistencia con el MER

    /**
     * Relación Many-to-Many con la entidad Permission, a través de la tabla intermedia 'role_permission'.
     * FetchType.EAGER se usa aquí para asegurar que los permisos se carguen junto con el rol,
     * lo cual puede ser útil al determinar las autoridades del usuario. Considera LAZY si tienes muchos permisos
     * y no siempre los necesitas inmediatamente.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permission", // Tabla de unión según el MER
        joinColumns = @JoinColumn(name = "role_id"), // FK a esta entidad (Role)
        inverseJoinColumns = @JoinColumn(name = "permission_id") // FK a la otra entidad (Permission)
    )
    @ToString.Exclude // Evitar recursión en toString con Lombok
    @EqualsAndHashCode.Exclude // Evitar recursión en equals/hashCode con Lombok
    private Set<Permission> permissions = new HashSet<>();

    // Constructor útil para crear roles con solo el nombre
    public Role(String roleName) {
        this.roleName = roleName;
    }

    // Si necesitas un constructor que también incluya permisos:
    // public Role(String roleName, Set<Permission> permissions) {
    //     this.roleName = roleName;
    //     this.permissions = permissions;
    // }
}