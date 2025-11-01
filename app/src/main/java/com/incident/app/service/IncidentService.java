package com.incident.app.service;

import java.time.LocalDateTime;
import java.util.List;

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
        Incident inc = new Incident();
        inc.setTitle(title);
        inc.setDescription(desc);
        inc.setSeverity(severity);
        inc.setStatus("OPEN");
        inc.setMessage(message);
        inc.setCreatedAt(LocalDateTime.now());
        inc.setUpdatedAt(LocalDateTime.now());
        return repo.save(inc);
    }

    @Transactional
    public Incident changeStatus(Long id, String newStatus, String message) {
        Incident inc = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Incident not found with id " + id));

        inc.setStatus(newStatus);

        if ("CLOSED".equalsIgnoreCase(newStatus)) {
            inc.setMessage(message != null ? message : "");
        } else {
            inc.setMessage(null);
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
}
