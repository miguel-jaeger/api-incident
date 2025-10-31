package com.incident.app.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.incident.app.model.Incident;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatus(String status);
}
