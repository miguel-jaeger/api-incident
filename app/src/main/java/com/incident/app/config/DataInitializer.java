package com.incident.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.incident.app.model.Incident;
import com.incident.app.repository.IncidentRepository;
import com.incident.app.service.IncidentService;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private IncidentRepository repo;

    @Autowired
    private IncidentService svc;

    @Override
    public void run(String... args) throws Exception {
        if (repo.count() == 0) {
            svc.create("Falla en proyector Epson Aula Magna Pabellón B", 
                "El proyector no enciende tras subida de tensión en el pabellón.", 
                "High", "pepitoooo", "Hardware", "Lima Centro - Aula Magna", "Inactive");

            svc.create("Caída enlace Wi-Fi Eduroam en Laboratorio 3", 
                "El access point AP-04 perdió conectividad con el switch de borde.", 
                "High", "", "Redes", "San Juan de Lurigancho - Pabellón A", "Active");

            svc.create("Reinicio no programado Servidor Canvas", 
                "Pico de carga provocó reinicio no programado del servicio.", 
                "High", "", "Software", "Data Center Campus Central", "Active");

            svc.create("Cable de red UTP desprendido en Rack", 
                "Patch cord desprendido en panel de parcheo principal.", 
                "Medium", "", "Infraestructura", "Lima Norte - Piso 3", "Active");

            svc.create("Error de autenticación LDAP en biblioteca", 
                "Terminales de consulta no validan credenciales de alumnos.", 
                "High", "probando", "Seguridad", "Arequipa - Biblioteca Central", "Active");

            svc.create("Cámara de vigilancia sin señal", 
                "Cámara IP de entrada principal no emite flujo RTSP al NVR.", 
                "Low", "Conector RJ45 reemplazado con éxito.", "Seguridad", "Chiclayo - Acceso Vehicular", "Inactive");
        }
    }
}
