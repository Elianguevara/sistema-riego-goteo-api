package com.sistemariegoagoteo.sistema_riego_goteo_api.config.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt") // <-- Cambiado de "custom.jwt" a "jwt" para coincidir con tu .properties
public class JwtConfig {
    private String secret; // <-- AÑADIDO
    private long expiration;

    // --- GETTERS Y SETTERS PARA AMBAS PROPIEDADES ---
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}