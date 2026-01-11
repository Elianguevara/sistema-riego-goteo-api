package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.TaskRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.TaskResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.TaskStatusUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Task;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * Crea una nueva tarea asignada a un operario.
     * Acceso restringido a Analistas.
     *
     * @param request Datos de la tarea (descripción, fecha límite, asignado a).
     * @return La tarea creada.
     */
    @PostMapping
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        Task createdTask = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TaskResponse(createdTask));
    }

    /**
     * Actualiza el estado de una tarea (ej. PENDIENTE -> COMPLETADA).
     * Acceso permitido a Operarios.
     *
     * @param taskId ID de la tarea.
     * @param request Nuevo estado y comentarios opcionales.
     * @return La tarea actualizada.
     */
    @PutMapping("/{taskId}/status")
    @PreAuthorize("hasRole('OPERARIO')")
    public ResponseEntity<TaskResponse> updateTaskStatus(@PathVariable Long taskId, @Valid @RequestBody TaskStatusUpdateRequest request) {
        Task updatedTask = taskService.updateTaskStatus(taskId, request);
        return ResponseEntity.ok(new TaskResponse(updatedTask));
    }

    /**
     * Obtiene las tareas asignadas al usuario actualmente autenticado (Operario).
     *
     * @param pageable Paginación (página y tamaño).
     * @return Página de tareas asignadas.
     */
    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasRole('OPERARIO')")
    public ResponseEntity<Page<TaskResponse>> getMyAssignedTasks(@PageableDefault(size = 20) Pageable pageable) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<TaskResponse> tasks = taskService.getTasksAssignedTo(currentUser, pageable).map(TaskResponse::new);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtiene las tareas creadas por el usuario actualmente autenticado (Analista).
     *
     * @param pageable Paginación (página y tamaño).
     * @return Página de tareas creadas.
     */
    @GetMapping("/created-by-me")
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<Page<TaskResponse>> getMyCreatedTasks(@PageableDefault(size = 20) Pageable pageable) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<TaskResponse> tasks = taskService.getTasksCreatedBy(currentUser, pageable).map(TaskResponse::new);
        return ResponseEntity.ok(tasks);
    }
}