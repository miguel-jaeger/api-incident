package com.incident.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // La raíz sirve directamente la página de Swagger (static/index.html),
        // por lo que la URL del navegador se mantiene limpia.
        registry.addViewController("/docs").setViewName("forward:/index.html");

        // Si alguien entra por las rutas internas de Swagger, vuelve a la raíz
        registry.addRedirectViewController("/swagger-ui.html", "/");
        registry.addRedirectViewController("/swagger-ui/index.html", "/");

        // Compatibilidad: la ruta antigua del contrato OpenAPI redirige a la nueva
        registry.addRedirectViewController("/v3/api-docs", "/openapi.json");
    }
}
