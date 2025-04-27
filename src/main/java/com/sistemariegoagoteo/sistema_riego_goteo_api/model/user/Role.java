package com.sistemariegoagoteo.sistema_riego_goteo_api.model.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entidad que representa un Rol en el sistema.
 * Mapea a la tabla 'roles'.
 */
@Data // Genera getters, setters, toString, equals, hashCode
@NoArgsConstructor // Genera constructor sin argumentos
@AllArgsConstructor // Genera constructor con todos los argumentos
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol") // Mapea a la columna 'id_rol' en la BD
    private Integer id;

    /**
     * Nombre único del rol (ej. "ADMIN", "ANALISTA", "OPERARIO").
     * Se usa para la lógica de autorización.
     */
    @Column(name = "nombre_rol", nullable = false, unique = true, length = 50)
    private String nombreRol;

    // Constructor útil para crear roles con solo el nombre
    public Role(String nombreRol) {
        this.nombreRol = nombreRol;
    }
}
