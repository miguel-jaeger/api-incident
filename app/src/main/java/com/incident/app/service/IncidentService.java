package com.incident.app.service;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import com.incident.app.model.Incident;
import com.incident.app.repository.IncidentRepository;

public class IncidentService {
    @Autowired
    IncidentRepository repo;

    public Incident create(String title, String desc, String severity) {
        Incident inc = new Incident();
        inc.setTitle(title);
        inc.setDescription(desc);
        inc.setSeverity(severity);
        inc.setStatus("OPEN");
        inc.setCreatedAt(LocalDateTime.now());
        inc.setUpdatedAt(LocalDateTime.now());
        return repo.save(inc);
    }

    public Incident changeStatus(Long id, String newStatus) {
        @SuppressWarnings("null")
        Incident inc = repo.findById(id).orElseThrow();
        inc.setStatus(newStatus);
        inc.setUpdatedAt(LocalDateTime.now());
        return repo.save(inc);
    }

    public List<Incident> listAll() {
        return repo.findAll();
    }
}
