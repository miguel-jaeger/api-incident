package com.incident.app.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.incident.app.dto.LoginRequest;
import com.incident.app.dto.LoginResponse;
import com.incident.app.dto.RegisterUserRequest;
import com.incident.app.dto.UserResponse;
import com.incident.app.model.User;
import com.incident.app.service.AuthSessionService;
import com.incident.app.service.UserService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthSessionService sessionService;

    public AuthController(UserService userService, AuthSessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        String identifier = req.getUsername() != null ? req.getUsername().trim() : "";
        String password = req.getPassword() != null ? req.getPassword() : "";

        // Autenticación contra la tabla de usuarios de SQLite
        User user = userService.authenticate(identifier, password);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(LoginResponse.failure("Usuario o contraseña incorrectos. Verifique sus credenciales."));
        }

        String token = sessionService.createSession(user);
        return ResponseEntity.ok(LoginResponse.success(
            token,
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getRole()
        ));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody RegisterUserRequest req) {
        try {
            User created = userService.register(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "authenticated", false,
                "message", "Sesión no válida o expirada."
            ));
        }

        User user = sessionUser.get();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("authenticated", true);
        body.put("username", user.getUsername());
        body.put("name", user.getName());
        if (user.getEmail() != null) {
            body.put("email", user.getEmail());
        }
        body.put("role", user.getRole());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        sessionService.invalidate(authHeader);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Sesión cerrada correctamente"
        ));
    }
}
