package com.barcelonaturisme.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger UI: /api/docs (a través del hub: /inventario/api/docs)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Inventario IT · Barcelona Turisme")
                .version("v1")
                .description("""
                        API del inventario del departamento de IT: ordenadores, usuarios e importación \
                        desde el Excel original.

                        Estados de un activo: EN_US, STOCK, REPARACIO, BAIXA. \
                        Estados de un usuario: ACTIU, BAIXA.

                        Los errores devuelven un Problem Detail (RFC 9457): el motivo está en el campo `detail`."""));
    }
}
