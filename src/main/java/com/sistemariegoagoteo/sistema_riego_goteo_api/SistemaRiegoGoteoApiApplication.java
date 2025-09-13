package com.sistemariegoagoteo.sistema_riego_goteo_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling; // <-- 1. Importar la anotación
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class SistemaRiegoGoteoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SistemaRiegoGoteoApiApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}