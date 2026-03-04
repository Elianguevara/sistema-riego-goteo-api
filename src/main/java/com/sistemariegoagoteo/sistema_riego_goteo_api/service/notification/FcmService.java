package com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Servicio para enviar push notifications a dispositivos Android via Firebase Cloud Messaging (FCM).
 *
 * Para activarlo:
 * 1. Crear un proyecto en https://console.firebase.google.com
 * 2. Ir a Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada
 * 3. Guardar el JSON descargado (ej. firebase-service-account.json) en src/main/resources/
 * 4. Configurar en application.properties: firebase.credentials.path=classpath:firebase-service-account.json
 */
@Service
@Slf4j
public class FcmService {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    private boolean initialized = false;

    @PostConstruct
    public void initialize() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("Firebase no configurado (firebase.credentials.path vacío). Push notifications desactivadas.");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            initialized = true;
            return;
        }

        try {
            InputStream serviceAccount;
            if (credentialsPath.startsWith("classpath:")) {
                String resourcePath = credentialsPath.substring("classpath:".length());
                serviceAccount = getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (serviceAccount == null) {
                    log.error("Archivo de credenciales Firebase no encontrado en classpath: {}", resourcePath);
                    return;
                }
            } else {
                serviceAccount = new FileInputStream(credentialsPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            initialized = true;
            log.info("Firebase Admin SDK inicializado correctamente.");

        } catch (IOException e) {
            log.error("Error inicializando Firebase Admin SDK: {}", e.getMessage());
        }
    }

    /**
     * Envía una push notification a un token FCM específico.
     *
     * @param fcmToken Token FCM del dispositivo destino.
     * @param title    Título de la notificación.
     * @param body     Cuerpo/mensaje de la notificación.
     */
    public void sendPushNotification(String fcmToken, String title, String body) {
        if (!initialized) {
            log.debug("FCM no inicializado, omitiendo push notification para token: {}", fcmToken != null ? fcmToken.substring(0, Math.min(10, fcmToken.length())) + "..." : "null");
            return;
        }

        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setToken(fcmToken)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification enviada exitosamente. FCM message ID: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("Error enviando push notification via FCM: {}", e.getMessage());
        }
    }
}
