package com.incident.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Al acceder a /docs, despacha internamente el index.html sin cambiar la barra de direcciones
        registry.addViewController("/docs").setViewName("forward:/swagger-ui/index.html");

        // (Opcional) Si alguien entra a la raíz "/", redirige a "/docs"
        registry.addRedirectViewController("/", "/docs");
    }
}