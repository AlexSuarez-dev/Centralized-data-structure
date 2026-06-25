package com.barcelonaturisme.inventory.config; // Adjust the package according to your current structure

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
                registry.addMapping("/api/**") // Applies to all endpoints of your API
                        .allowedOrigins(
                            "http://svdocker:5500",                                   
                            "http://localhost:5500",  
                            "http://127.0.0.1:5500"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed methods
                        .allowedHeaders("*") 
                        .allowCredentials(true); 
            }
        };
    }
}