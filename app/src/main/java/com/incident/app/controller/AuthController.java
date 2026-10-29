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

import jakarta.persistence.EntityNotFoundException;

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
    public ResponseEntity<?> createUser(
            @RequestBody RegisterUserRequest req,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        boolean isAdmin = sessionUser
            .map(user -> "ADMIN".equalsIgnoreCase(user.getRole()))
            .orElse(false);

        String requestedRole = req.getRole() != null ? req.getRole().trim() : "";
        if (!isAdmin) {
            // El registro público no puede elevar privilegios
            if (!requestedRole.isEmpty() && !"OPERATOR".equalsIgnoreCase(requestedRole)) {
                return error(HttpStatus.FORBIDDEN, "Solo el administrador puede asignar roles distintos de OPERATOR.");
            }
            req.setRole("OPERATOR");
        }

        try {
            User created = userService.register(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para consultar usuarios.");
        }
        if (!"ADMIN".equalsIgnoreCase(sessionUser.get().getRole())) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede consultar usuarios.");
        }

        try {
            return ResponseEntity.ok(UserResponse.from(userService.findById(id)));
        } catch (EntityNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para cambiar el rol de un usuario.");
        }
        if (!"ADMIN".equalsIgnoreCase(sessionUser.get().getRole())) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede cambiar el rol de los usuarios.");
        }

        return applyRoleChange(id, body.get("role"));
    }

    @RequestMapping(value = "/roles", method = { RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST })
    public ResponseEntity<?> updateRole(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para cambiar el rol de un usuario.");
        }
        if (!"ADMIN".equalsIgnoreCase(sessionUser.get().getRole())) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede cambiar el rol de los usuarios.");
        }

        Long userId = parseLong(body.get("userId") != null ? body.get("userId") : body.get("id"));
        if (userId == null && body.get("username") != null) {
            try {
                userId = userService.findByUsername(String.valueOf(body.get("username"))).getId();
            } catch (EntityNotFoundException e) {
                return error(HttpStatus.NOT_FOUND, e.getMessage());
            }
        }
        if (userId == null) {
            return error(HttpStatus.BAD_REQUEST, "El campo 'userId' (o 'id') es obligatorio.");
        }

        Object roleValue = body.get("role") != null ? body.get("role") : body.get("newRole");
        String role = roleValue != null ? String.valueOf(roleValue) : null;
        return applyRoleChange(userId, role);
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

    private ResponseEntity<?> applyRoleChange(Long userId, String role) {
        try {
            User updated = userService.changeRole(userId, role);
            return ResponseEntity.ok(UserResponse.from(updated));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (EntityNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "status", "error",
            "message", message
        ));
    }
}
