package com.incident.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.incident.app.service.IncidentService;
import com.incident.app.model.Incident;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    @Autowired
    IncidentService svc;

    @PostMapping()
    public Incident create(@RequestBody Map<String, String> body) {
        return svc.create(body.get("title"), body.get("description"), body.get("severity"));
    }

    @PatchMapping("/{id}/status")
    public Incident updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return svc.changeStatus(id, body.get("status"));
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
    public void delete(@PathVariable Long id) {
        svc.delete(id);
    }
}