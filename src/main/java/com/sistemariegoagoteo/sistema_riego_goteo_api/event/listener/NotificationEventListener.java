package com.sistemariegoagoteo.sistema_riego_goteo_api.event.listener;

import com.sistemariegoagoteo.sistema_riego_goteo_api.event.HumidityAlertCreatedEvent;
import com.sistemariegoagoteo.sistema_riego_goteo_api.event.MaintenanceCreatedEvent;
import com.sistemariegoagoteo.sistema_riego_goteo_api.event.TaskAssignedEvent;
import com.sistemariegoagoteo.sistema_riego_goteo_api.event.TaskStatusUpdatedEvent;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.AppNotification;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.NotificationType;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.notification.NotificationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskAssignedEvent(TaskAssignedEvent event) {
        userRepository.findById(event.assignedToUserId()).ifPresent(user -> {
            AppNotification notif = new AppNotification();
            notif.setDestinatario(user);
            notif.setType(NotificationType.INFO);
            notif.setMessage("Nueva tarea asignada: " + event.description());
            notif.setEntityType("TASK");
            notif.setEntityId(event.taskId());
            notif.setActionUrl("/tasks/assigned-to-me/" + event.taskId());

            notificationRepository.save(notif);
            log.info("AppNotification persistida para el usuario {} sobre asignación de tarea {}", user.getUsername(),
                    event.taskId());
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskStatusUpdatedEvent(TaskStatusUpdatedEvent event) {
        userRepository.findById(event.createdByUserId()).ifPresent(user -> {
            AppNotification notif = new AppNotification();
            notif.setDestinatario(user);
            notif.setType(NotificationType.SUCCESS);
            notif.setMessage(
                    "El operario " + event.operatorName() + " actualizó la tarea a estado: " + event.statusName());
            notif.setEntityType("TASK");
            notif.setEntityId(event.taskId());
            notif.setActionUrl("/tasks/created-by-me/" + event.taskId());

            notificationRepository.save(notif);
            log.info("AppNotification persistida para el analista {} sobre actualización de tarea {}",
                    user.getUsername(), event.taskId());
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHumidityAlertCreatedEvent(HumidityAlertCreatedEvent event) {
        // Notificar a administradores o analistas de la finca (simplificado a buscar
        // admins/analistas para este ejemplo)
        userRepository.findAll().stream()
                .filter(u -> u.getRol().getRoleName().equals("ADMIN") || u.getRol().getRoleName().equals("ANALISTA"))
                .forEach(user -> {
                    AppNotification notif = new AppNotification();
                    notif.setDestinatario(user);
                    notif.setType(NotificationType.WARNING);
                    notif.setMessage("Alerta en " + event.sensorName() + ": Nivel de humedad crítico del "
                            + event.humidityLevel() + "%");
                    notif.setEntityType("ALERT");
                    notif.setEntityId(event.alertId().longValue());
                    notif.setActionUrl("/farms/" + event.farmId() + "/alerts/" + event.alertId());

                    notificationRepository.save(notif);
                });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMaintenanceCreatedEvent(MaintenanceCreatedEvent event) {
        userRepository.findAll().stream()
                .filter(u -> u.getRol().getRoleName().equals("ADMIN") || u.getRol().getRoleName().equals("ANALISTA"))
                .forEach(user -> {
                    AppNotification notif = new AppNotification();
                    notif.setDestinatario(user);
                    notif.setType(NotificationType.INFO);
                    notif.setMessage(
                            "Mantenimiento registrado en " + event.equipmentName() + ": " + event.description());
                    notif.setEntityType("MAINTENANCE");
                    notif.setEntityId(event.maintenanceId().longValue());
                    notif.setActionUrl("/farms/" + event.farmId() + "/maintenances/" + event.maintenanceId());

                    notificationRepository.save(notif);
                });
    }
}
