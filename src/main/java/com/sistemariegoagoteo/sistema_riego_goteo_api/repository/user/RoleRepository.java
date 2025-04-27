package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;

import java.util.Optional;

/**
 * Repositorio para la entidad Role.
 * Provee métodos CRUD básicos y la capacidad de buscar por nombre de rol.
 */
@Repository // Indica que es un componente repositorio de Spring
public interface RoleRepository extends JpaRepository<Role, Integer> { // <Entidad, Tipo de ID>

    /**
     * Busca un rol por su nombre único.
     * Spring Data JPA genera la implementación automáticamente basado en el nombre del método.
     *
     * @param nombreRol El nombre del rol a buscar (ej. "ADMIN").
     * @return Un Optional que contiene el Role si se encuentra, o vacío si no.
     */
    Optional<Role> findByNombreRol(String nombreRol);
}
