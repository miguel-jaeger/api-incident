package com.incident.app.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.incident.app.model.Incident;
import com.incident.app.model.User;
import com.incident.app.service.AuthSessionService;
import com.incident.app.service.IncidentService;

import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/incidents")
@Tag(name = "Incidentes", description = "Gestión de incidentes y estadísticas")
public class IncidentController {

    @Autowired
    private IncidentService svc;

    @Autowired
    private AuthSessionService sessionService;

    // Cualquier usuario autenticado puede publicar un incidente
    @PostMapping()
    public ResponseEntity<?> create(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para publicar un incidente.");
        }

        String title = body.get("title") != null ? String.valueOf(body.get("title")) : "";
        String description = body.get("description") != null ? String.valueOf(body.get("description")) : "";

        String severity = body.get("severity") != null
            ? String.valueOf(body.get("severity"))
            : (body.get("priority") != null ? String.valueOf(body.get("priority")) : "Low");

        String message = body.get("message") != null
            ? String.valueOf(body.get("message"))
            : (body.get("rejectionReason") != null ? String.valueOf(body.get("rejectionReason")) : "");

        String category = body.get("category") != null ? String.valueOf(body.get("category")) : "General";
        String location = body.get("location") != null ? String.valueOf(body.get("location")) : "Campus UTP";

        // Solo el administrador puede definir el estado al crear; el resto inicia en Active
        boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionUser.get().getRole());
        String status = isAdmin && body.get("status") != null
            ? String.valueOf(body.get("status"))
            : "Active";

        Incident created = svc.create(title, description, severity, message, category, location, status);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Solo el administrador puede actualizar el estado de un incidente
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para actualizar el estado de un incidente.");
        }
        if (!"ADMIN".equalsIgnoreCase(sessionUser.get().getRole())) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede actualizar el estado de un incidente.");
        }

        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "El campo 'status' es obligatorio.");
        }

        String message = body.get("message") != null ? body.get("message") : body.get("rejectionReason");
        return ResponseEntity.ok(svc.changeStatus(id, status, message));
    }

    // Solo el administrador puede actualizar la prioridad de un incidente
    @PatchMapping("/{id}/priority")
    public ResponseEntity<?> updatePriority(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Optional<User> sessionUser = sessionService.findByAuthorizationHeader(authHeader);
        if (sessionUser.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para actualizar la prioridad de un incidente.");
        }
        if (!"ADMIN".equalsIgnoreCase(sessionUser.get().getRole())) {
            return error(HttpStatus.FORBIDDEN, "Solo el administrador puede actualizar la prioridad de un incidente.");
        }

        String priority = body.get("priority") != null ? body.get("priority") : body.get("severity");
        if (priority == null || priority.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "El campo 'priority' es obligatorio.");
        }

        return ResponseEntity.ok(svc.changePriority(id, priority));
    }

    @GetMapping()
    public List<Incident> all() {
        return svc.listAll();
    }

    @GetMapping("/{id}")
    public Incident getById(@PathVariable Long id) {
        return svc.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ====== ENDPOINTS DE ESTADÍSTICAS Y MÉTRICAS ======

    @GetMapping("/stats/count")
    public Map<String, Long> getCount() {
        return Map.of("total", svc.countTotal());
    }

    @GetMapping("/stats/total")
    public Map<String, Long> getTotalStats() {
        return Map.of("total", svc.countTotal());
    }

    @GetMapping("/stats/by-category")
    public Map<String, Long> getCountByCategory() {
        return svc.countByCategory();
    }

    @GetMapping("/stats/by-severity")
    public Map<String, Long> getCountBySeverity() {
        return svc.countBySeverity();
    }

    @GetMapping("/stats/by-status")
    public Map<String, Long> getCountByStatus() {
        return svc.countByStatus();
    }

    @GetMapping("/stats")
    public Map<String, Object> getOverallStats() {
        return svc.getOverallStats();
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "status", "error",
            "message", message
        ));
    }
}
