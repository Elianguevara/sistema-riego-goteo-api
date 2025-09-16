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

    // AÑADE ESTE MÉTODO
    @PostConstruct
    public void init(){
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(SistemaRiegoGoteoApiApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}