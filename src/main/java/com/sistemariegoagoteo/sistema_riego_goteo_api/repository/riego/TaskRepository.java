package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Task;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.TaskStatus; // Importar TaskStatus
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Para que un operario vea sus tareas
    Page<Task> findByAssignedToOrderByCreatedAtDesc(User assignedTo, Pageable pageable);

    // Para que un analista vea las tareas que ha creado
    Page<Task> findByCreatedByOrderByCreatedAtDesc(User createdBy, Pageable pageable);

    // --- MÉTODOS NUEVOS AÑADIDOS ---

    /**
     * Cuenta el total de tareas creadas por un usuario específico.
     */
    long countByCreatedBy(User creator);

    /**
     * Cuenta las tareas creadas por un usuario específico que tienen un estado determinado.
     */
    long countByCreatedByAndStatus(User creator, TaskStatus status);
}