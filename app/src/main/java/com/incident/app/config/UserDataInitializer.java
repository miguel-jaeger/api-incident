package com.incident.app.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.incident.app.dto.RegisterUserRequest;
import com.incident.app.repository.UserRepository;
import com.incident.app.service.UserService;

/**
 * Carga los usuarios iniciales del sistema en SQLite.
 * Si el usuario ya existe, no se vuelve a crear.
 */
@Component
public class UserDataInitializer implements CommandLineRunner {

    private final UserRepository repo;
    private final UserService userService;

    public UserDataInitializer(UserRepository repo, UserService userService) {
        this.repo = repo;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        seed("admin", "admin@utp.edu.pe", "admin123", "Admin TI UTP", "ADMIN");
        seed("supervisor", "C32007@utp.edu.pe", "utp2025", "Docente Miguel Jaeger", "SUPERVISOR");
        seed("operador", "operador@utp.edu.pe", "operador123", "Operador NOC Campus", "OPERATOR");
        seed("utp", "soporte@utp.edu.pe", "utp2025", "Mesa de Ayuda UTP", "SUPPORT");
    }

    private void seed(String username, String email, String password, String name, String role) {
        if (repo.existsByUsernameIgnoreCase(username) || repo.existsByEmailIgnoreCase(email)) {
            return;
        }

        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername(username);
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(role);

        userService.register(request);
    }
}
