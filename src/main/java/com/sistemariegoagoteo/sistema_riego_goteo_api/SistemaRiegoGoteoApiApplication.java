package com.sistemariegoagoteo.sistema_riego_goteo_api;

import jakarta.annotation.PostConstruct; // Importa esta clase
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import java.util.TimeZone; // Importa esta clase

@SpringBootApplication
@EnableScheduling
public class SistemaRiegoGoteoApiApplication {




    public static void main(String[] args) {
        SpringApplication.run(SistemaRiegoGoteoApiApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Establece la zona horaria por defecto a la de Argentina
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}