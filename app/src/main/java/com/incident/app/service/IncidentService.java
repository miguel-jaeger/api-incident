package com.incident.app.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.incident.app.model.Incident;
import com.incident.app.repository.IncidentRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository repo;

    @PersistenceContext
    private EntityManager em;

    public Incident create(String title, String desc, String severity, String message) {
        return create(title, desc, severity, message, "General", "Campus UTP", "OPEN");
    }

    public Incident create(String title, String desc, String severity, String message, String category, String location, String status) {
        Incident inc = new Incident();
        inc.setTitle(title);
        inc.setDescription(desc);
        inc.setSeverity(severity != null && !severity.trim().isEmpty() ? severity : "Medium");
        inc.setStatus(status != null && !status.trim().isEmpty() ? status : "Active");
        inc.setMessage(message);
        inc.setCategory(category != null && !category.trim().isEmpty() ? category : "General");
        inc.setLocation(location != null && !location.trim().isEmpty() ? location : "Campus UTP");
        inc.setCreatedAt(LocalDateTime.now());
        inc.setUpdatedAt(LocalDateTime.now());
        return repo.save(inc);
    }

    @Transactional
    public Incident changeStatus(Long id, String newStatus, String message) {
        Incident inc = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Incident not found with id " + id));

        inc.setStatus(newStatus);

        if ("CLOSED".equalsIgnoreCase(newStatus) || "Inactive".equalsIgnoreCase(newStatus)) {
            inc.setMessage(message != null ? message : "");
        } else {
            inc.setMessage(message != null ? message : null);
        }

        inc.setUpdatedAt(LocalDateTime.now());
        repo.save(inc);

        // Forzar sincronización con la base de datos
        em.flush();
        em.clear();

        // Devolver entidad actualizada desde la base
        return repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Incident not found after update"));
    }

    public List<Incident> listAll() {
        return repo.findAll();
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public Incident findById(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Incident not found with id " + id));
    }

    public long countTotal() {
        return repo.count();
    }

    public Map<String, Long> countByCategory() {
        List<Incident> all = repo.findAll();
        return all.stream()
            .collect(Collectors.groupingBy(
                i -> (i.getCategory() != null && !i.getCategory().trim().isEmpty()) ? i.getCategory() : "General",
                LinkedHashMap::new,
                Collectors.counting()
            ));
    }

    public Map<String, Long> countBySeverity() {
        List<Incident> all = repo.findAll();
        return all.stream()
            .collect(Collectors.groupingBy(
                i -> (i.getSeverity() != null && !i.getSeverity().trim().isEmpty()) ? i.getSeverity() : "Medium",
                LinkedHashMap::new,
                Collectors.counting()
            ));
    }

    public Map<String, Long> countByStatus() {
        List<Incident> all = repo.findAll();
        return all.stream()
            .collect(Collectors.groupingBy(
                i -> (i.getStatus() != null && !i.getStatus().trim().isEmpty()) ? i.getStatus() : "Active",
                LinkedHashMap::new,
                Collectors.counting()
            ));
    }

    public Map<String, Object> getOverallStats() {
        List<Incident> all = repo.findAll();
        long total = all.size();

        Map<String, Long> byCategory = countByCategory();
        Map<String, Long> bySeverity = countBySeverity();
        Map<String, Long> byStatus = countByStatus();

        long activeCount = all.stream().filter(i -> {
            String s = i.getStatus();
            return s == null || "Active".equalsIgnoreCase(s) || "OPEN".equalsIgnoreCase(s) || "IN_PROGRESS".equalsIgnoreCase(s);
        }).count();

        long inactiveCount = all.stream().filter(i -> {
            String s = i.getStatus();
            return s != null && ("Inactive".equalsIgnoreCase(s) || "CLOSED".equalsIgnoreCase(s) || "RESOLVED".equalsIgnoreCase(s));
        }).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("active", activeCount);
        stats.put("inactive", inactiveCount);
        stats.put("byCategory", byCategory);
        stats.put("bySeverity", bySeverity);
        stats.put("byStatus", byStatus);

        return stats;
    }
}
