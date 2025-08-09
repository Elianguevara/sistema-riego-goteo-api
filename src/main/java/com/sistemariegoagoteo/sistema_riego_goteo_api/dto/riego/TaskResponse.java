package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Task;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.TaskStatus;
import lombok.Data;
import java.util.Date;

@Data
public class TaskResponse {
    private Long id;
    private String description;
    private TaskStatus status;
    private String createdByUsername;
    private String assignedToUsername;
    private Date createdAt;
    private Date updatedAt;

    public TaskResponse(Task task) {
        this.id = task.getId();
        this.description = task.getDescription();
        this.status = task.getStatus();
        if (task.getCreatedBy() != null) {
            this.createdByUsername = task.getCreatedBy().getUsername();
        }
        if (task.getAssignedTo() != null) {
            this.assignedToUsername = task.getAssignedTo().getUsername();
        }
        this.createdAt = task.getCreatedAt();
        this.updatedAt = task.getUpdatedAt();
    }
}