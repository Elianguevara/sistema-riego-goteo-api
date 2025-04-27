package com.sistemariegoagoteo.sistema_riego_goteo_api.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin"; // **Cambia esto por la contraseña que quieres usar**
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Contraseña encriptada para '" + rawPassword + "': " + encodedPassword);
        // Copia el output de esta línea y úsalo en tu script SQL
    }
}
