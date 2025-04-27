package com.sistemariegoagoteo.sistema_riego_goteo_api.util;

import java.security.SecureRandom;
import java.util.Base64;

public class GenerateSecretKey {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[64]; // 64 bytes = 512 bits (bueno para HS512)
        random.nextBytes(keyBytes);
        String base64EncodedKey = Base64.getEncoder().encodeToString(keyBytes);
        System.out.println("Clave Base64 generada: " + base64EncodedKey);
    }
}
