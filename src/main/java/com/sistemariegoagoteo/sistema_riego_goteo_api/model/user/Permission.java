package com.sistemariegoagoteo.sistema_riego_goteo_api.model.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
// import java.util.Set; // No es necesario aquí a menos que Permission tenga una relación ManyToMany inversa a Role

/**
 * Entidad que representa un Permiso en el sistema.
 * Mapea a la tabla 'permission' del MER.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permission") // Nombre de la tabla según el MER
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id") // Columna 'permission_id' en el MER
    private Integer id;

    @Column(name = "permission_name", nullable = false, unique = true, length = 50) // Columna 'permission_name' en el MER
    private String permissionName;

    // Si la relación con Role fuera bidireccional (es decir, si desde Permission quisieras acceder a los Roles que tienen este permiso),
    // añadirías un @ManyToMany(mappedBy = "permissions") aquí.
    // Por ahora, la relación está definida unidireccionalmente desde Role.
    // @ManyToMany(mappedBy = "permissions")
    // private Set<Role> roles = new HashSet<>();

    // Constructor útil
    public Permission(String permissionName) {
        this.permissionName = permissionName;
    }
}