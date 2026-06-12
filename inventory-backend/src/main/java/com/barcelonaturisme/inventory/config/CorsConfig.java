package com.barcelonaturisme.inventory.config; // Ajusta el package según tu estructura actual

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Aplica a todos los endpoints de tu API
                        .allowedOrigins(
                            "http://localhost",       // Tu nuevo frontend en Docker (Nginx)
                            "http://127.0.0.1",       // Alternativa de Docker
                            "http://localhost:5500",  // Por si vuelves a usar Live Server
                            "http://127.0.0.1:5500"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Verbos permitidos
                        .allowedHeaders("*") // Permite cualquier cabecera
                        .allowCredentials(true); // Permite enviar cookies o tokens si los tuvieras
            }
        };
    }
}