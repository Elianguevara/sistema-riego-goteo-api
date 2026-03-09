package com.sistemariegoagoteo.sistema_riego_goteo_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Riego a Goteo - API")
                        .description("API REST para la gestión de riego agrícola por goteo. " +
                                "Permite administrar fincas, sectores, equipos, sensores de humedad, " +
                                "fertilizaciones, riegos, tareas y notificaciones.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Tecnicatura en Desarrollo de Software")
                                .email("contacto@sistemariego.com")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresá el token JWT obtenido en POST /api/auth/login")));
    }
}
