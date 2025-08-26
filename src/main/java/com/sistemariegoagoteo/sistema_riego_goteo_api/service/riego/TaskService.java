package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.TaskRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.TaskStatusUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector; // <-- NUEVO: Importar Sector
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Task;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository; // <-- NUEVO: Importar SectorRepository
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.TaskRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SectorRepository sectorRepository; // <-- NUEVO: Inyectar el repositorio de Sector

    @Transactional
    public Task createTask(TaskRequest request) {
        User creator = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User assignee = userRepository.findById(request.getAssignedToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedToUserId()));

        if (!"OPERARIO".equals(assignee.getRol().getRoleName())) {
            throw new IllegalArgumentException("Las tareas solo pueden ser asignadas a usuarios con rol OPERARIO.");
        }

        // --- LÓGICA CORREGIDA ---
        // 1. Buscar el Sector usando el ID que viene en el request
        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", request.getSectorId()));

        Task task = new Task();
        task.setDescription(request.getDescription());
        task.setCreatedBy(creator);
        task.setAssignedTo(assignee);
        task.setSector(sector); // 2. Asignar el objeto Sector encontrado a la tarea

        Task savedTask = taskRepository.save(task);
        log.info("Analista {} ha creado la tarea {} para el operario {}", creator.getUsername(), savedTask.getId(), assignee.getUsername());

        // --- NOTIFICACIÓN AL OPERARIO ---
        String message = String.format("Nueva tarea asignada por %s: \"%s\"", creator.getName(), savedTask.getDescription());
        String link = "/tasks/assigned-to-me/" + savedTask.getId();
        notificationService.createNotification(assignee, message, link);

        return savedTask;
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatusUpdateRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (!task.getAssignedTo().getId().equals(currentUser.getId())) {
            throw new SecurityException("No tienes permiso para actualizar el estado de esta tarea.");
        }

        task.setStatus(request.getStatus());
        Task updatedTask = taskRepository.save(task);
        log.info("Operario {} ha actualizado el estado de la tarea {} a {}", currentUser.getUsername(), taskId, request.getStatus());

        // --- NOTIFICACIÓN AL ANALISTA ---
        String message = String.format("El operario %s ha actualizado el estado de tu tarea a: %s", currentUser.getName(), updatedTask.getStatus());
        String link = "/tasks/created-by-me/" + updatedTask.getId();
        notificationService.createNotification(task.getCreatedBy(), message, link);

        return updatedTask;
    }

    @Transactional(readOnly = true)
    public Page<Task> getTasksAssignedTo(User user, Pageable pageable) {
        return taskRepository.findByAssignedToOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Task> getTasksCreatedBy(User user, Pageable pageable) {
        return taskRepository.findByCreatedByOrderByCreatedAtDesc(user, pageable);
    }
}