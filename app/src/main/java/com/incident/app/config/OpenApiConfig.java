package com.incident.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("API de Gestión de Incidentes")
                .version("1.0.0")
                .description("Documentación oficial y entorno de pruebas para los endpoints de la API.")
                .termsOfService("https://api-incident.onrender.com/docs")
                .contact(new Contact()
                    .name("Soporte Técnico")
                    .email("miguel.jaeger@gmail.com")
                    .url("https://api-incident.onrender.com/"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server().url("/").description("Servidor por defecto")
            ));
    }
}