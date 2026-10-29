package com.incident.app.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.incident.app.dto.LoginRequest;
import com.incident.app.dto.LoginResponse;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        String user = req.getUsername() != null ? req.getUsername().trim() : "";
        String pass = req.getPassword() != null ? req.getPassword().trim() : "";

        // Credenciales válidas para el sistema de Mesa Operativa Campus UTP
        if (("admin".equalsIgnoreCase(user) || "admin@utp.edu.pe".equalsIgnoreCase(user)) && "admin123".equals(pass)) {
            String token = "utp-token-" + UUID.randomUUID().toString();
            return ResponseEntity.ok(LoginResponse.success(token, "admin", "Admin TI UTP", "admin@utp.edu.pe", "ADMIN"));
        }

        if (("supervisor".equalsIgnoreCase(user) || "C32007@utp.edu.pe".equalsIgnoreCase(user)) && ("utp2025".equals(pass) || "admin123".equals(pass))) {
            String token = "utp-token-" + UUID.randomUUID().toString();
            return ResponseEntity.ok(LoginResponse.success(token, "supervisor", "Docente Miguel Jaeger", "C32007@utp.edu.pe", "SUPERVISOR"));
        }

        if (("operador".equalsIgnoreCase(user) || "operador@utp.edu.pe".equalsIgnoreCase(user)) && "operador123".equals(pass)) {
            String token = "utp-token-" + UUID.randomUUID().toString();
            return ResponseEntity.ok(LoginResponse.success(token, "operador", "Operador NOC Campus", "operador@utp.edu.pe", "OPERATOR"));
        }

        if (("utp".equalsIgnoreCase(user) || "soporte@utp.edu.pe".equalsIgnoreCase(user)) && "utp2025".equals(pass)) {
            String token = "utp-token-" + UUID.randomUUID().toString();
            return ResponseEntity.ok(LoginResponse.success(token, "utp", "Mesa de Ayuda UTP", "soporte@utp.edu.pe", "SUPPORT"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(LoginResponse.failure("Usuario o contraseña incorrectos. Verifique sus credenciales."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", "admin",
                "name", "Admin TI UTP",
                "email", "admin@utp.edu.pe",
                "role", "ADMIN"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "username", "admin",
            "name", "Admin TI UTP",
            "role", "ADMIN"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Sesión cerrada correctamente"
        ));
    }
}
