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
 * <p>
 * Define un conjunto de permisos y autoridades que pueden ser asignados a los
 * usuarios.
 * Los roles típicos incluyen "ADMIN", "ANALISTA" y "OPERARIO".
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role")
public class Role {

    /**
     * Identificador único del rol.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer id;

    /**
     * Nombre único del rol.
     * Se utiliza en la lógica de seguridad para restringir el acceso a endpoints y
     * servicios.
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    /**
     * Relación Many-to-Many con la entidad Permission, a través de la tabla
     * intermedia 'role_permission'.
     * FetchType.EAGER se usa aquí para asegurar que los permisos se carguen junto
     * con el rol,
     * lo cual puede ser útil al determinar las autoridades del usuario. Considera
     * LAZY si tienes muchos permisos
     * y no siempre los necesitas inmediatamente.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permission", // Tabla de unión según el MER
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
    // this.roleName = roleName;
    // this.permissions = permissions;
    // }
}