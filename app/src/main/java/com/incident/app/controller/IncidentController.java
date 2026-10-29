package com.incident.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.incident.app.service.IncidentService;
import com.incident.app.model.Incident;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    @Autowired
    private IncidentService svc;

    @PostMapping()
    public Incident create(@RequestBody Map<String, Object> body) {
        String title = body.get("title") != null ? String.valueOf(body.get("title")) : "";
        String description = body.get("description") != null ? String.valueOf(body.get("description")) : "";
        
        String severity = body.get("severity") != null 
            ? String.valueOf(body.get("severity")) 
            : (body.get("priority") != null ? String.valueOf(body.get("priority")) : "Medium");

        String message = body.get("message") != null 
            ? String.valueOf(body.get("message")) 
            : (body.get("rejectionReason") != null ? String.valueOf(body.get("rejectionReason")) : "");

        String category = body.get("category") != null ? String.valueOf(body.get("category")) : "General";
        String location = body.get("location") != null ? String.valueOf(body.get("location")) : "Campus UTP";
        String status = body.get("status") != null ? String.valueOf(body.get("status")) : "Active";

        return svc.create(title, description, severity, message, category, location, status);
    }

    @PatchMapping("/{id}/status")
    public Incident updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String message = body.get("message") != null ? body.get("message") : body.get("rejectionReason");
        return svc.changeStatus(id, status, message);
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
}
