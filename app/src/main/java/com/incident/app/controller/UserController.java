package com.incident.app.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.incident.app.dto.RegisterUserRequest;
import com.incident.app.dto.UserResponse;
import com.incident.app.model.User;
import com.incident.app.service.AuthSessionService;
import com.incident.app.service.UserService;

import jakarta.persistence.EntityNotFoundException;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthSessionService sessionService;

    public UserController(UserService userService, AuthSessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    // ====== CONSULTA DE USUARIOS ======

    @GetMapping
    public ResponseEntity<?> all(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para consultar usuarios.");
        }
        if (!isAdmin(sessionUser)) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede consultar usuarios.");
        }

        List<UserResponse> users = userService.listAll().stream()
            .map(UserResponse::from)
            .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para consultar usuarios.");
        }
        if (!isAdmin(sessionUser)) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede consultar usuarios.");
        }

        try {
            return ResponseEntity.ok(UserResponse.from(userService.findById(id)));
        } catch (EntityNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // ====== CREACIÓN DE USUARIOS ======

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody RegisterUserRequest req,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        boolean admin = sessionUser
            .map(user -> "ADMIN".equalsIgnoreCase(user.getRole()))
            .orElse(false);

        String requestedRole = req.getRole() != null ? req.getRole().trim() : "";
        if (!admin) {
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

    // ====== CAMBIO DE ROL ======

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para cambiar el rol de un usuario.");
        }
        if (!isAdmin(sessionUser)) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede cambiar el rol de los usuarios.");
        }

        return applyRoleChange(id, body.get("role"));
    }

    // ====== CAMBIO DE CONTRASEÑA ======

    @PatchMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para cambiar una contraseña.");
        }

        boolean self = sessionUser.get().getId().equals(id);
        if (!self && !isAdmin(sessionUser)) {
            return error(HttpStatus.FORBIDDEN, "Solo puedes cambiar tu propia contraseña o debes ser administrador.");
        }

        String currentPassword = body.get("currentPassword") != null
            ? body.get("currentPassword")
            : body.get("oldPassword");
        String newPassword = body.get("newPassword") != null
            ? body.get("newPassword")
            : body.get("password");

        try {
            // Para la propia contraseña se exige la actual; un ADMIN puede cambiarla a otro usuario sin ella.
            User updated = userService.changePassword(id, currentPassword, newPassword, self);
            return ResponseEntity.ok(UserResponse.from(updated));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (EntityNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // ====== ELIMINACIÓN DE USUARIOS ======

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para eliminar usuarios.");
        }
        if (!isAdmin(sessionUser)) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede eliminar usuarios.");
        }

        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // ====== APOYO ======

    private boolean isAdmin(Optional<User> sessionUser) {
        return sessionUser
            .map(user -> "ADMIN".equalsIgnoreCase(user.getRole()))
            .orElse(false);
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

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "status", "error",
            "message", message
        ));
    }
}
