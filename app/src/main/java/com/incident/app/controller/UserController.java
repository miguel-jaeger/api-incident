package com.incident.app.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.incident.app.dto.UserResponse;
import com.incident.app.model.User;
import com.incident.app.service.AuthSessionService;
import com.incident.app.service.UserService;

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

    @GetMapping
    public ResponseEntity<?> all(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para consultar usuarios.");
        }
        if (!"ADMIN".equalsIgnoreCase(sessionUser.get().getRole())) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede consultar usuarios.");
        }

        List<UserResponse> users = userService.listAll().stream()
            .map(UserResponse::from)
            .toList();
        return ResponseEntity.ok(users);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "status", "error",
            "message", message
        ));
    }
}
