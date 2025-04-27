package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user;

// Añadir imports necesarios
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Importar List
import java.util.Optional;

/**
 * Repositorio para la entidad User.
 * Provee métodos CRUD básicos y métodos personalizados para buscar por username, email y rol.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su nombre de usuario (username) único.
     *
     * @param username El nombre de usuario a buscar.
     * @return Un Optional que contiene el User si se encuentra, o vacío si no.
     */
    Optional<User> findByUsername(String username);

    /**
     * Busca un usuario por su dirección de correo electrónico (email) única.
     *
     * @param email El email a buscar.
     * @return Un Optional que contiene el User si se encuentra, o vacío si no.
     */
    Optional<User> findByEmail(String email);

    /**
     * Comprueba si existe un usuario con el nombre de usuario dado.
     *
     * @param username El nombre de usuario a comprobar.
     * @return true si existe un usuario con ese username, false en caso contrario.
     */
    boolean existsByUsername(String username);

    /**
     * Comprueba si existe un usuario con el email dado.
     *
     * @param email El email a comprobar.
     * @return true si existe un usuario con ese email, false en caso contrario.
     */
    boolean existsByEmail(String email);

    /**
     * Busca todos los usuarios que tienen asignado un rol específico.
     * Spring Data JPA genera la consulta automáticamente basado en el nombre del método.
     *
     * @param role El objeto Role por el cual filtrar.
     * @return Una lista de usuarios que tienen ese rol.
     */
    List<User> findByRol(Role role); // <-- Método añadido

}
