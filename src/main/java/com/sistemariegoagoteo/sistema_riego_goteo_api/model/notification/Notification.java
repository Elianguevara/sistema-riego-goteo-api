package com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient; // El usuario que recibe la notificación

    @Column(nullable = false)
    private String message; // ej: "Alerta de baja humedad en Sector 3"

    private boolean isRead = false; // Para saber si el usuario ya la vio

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt;

    private String link; // Opcional: una ruta del frontend para redirigir al usuario
}
