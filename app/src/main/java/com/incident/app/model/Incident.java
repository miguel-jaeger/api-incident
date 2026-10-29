package com.incident.app.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
public class Incident {
    @Id @GeneratedValue
    private Long id;
    private String title;
    private String description;
    private String message;
    private String status; // por ejemplo: OPEN, IN_PROGRESS, RESOLVED, CLOSED, Active, Inactive
    private String severity; // High, Medium, Low
    private String category; // Redes, Hardware, Software, Seguridad, Infraestructura, General
    private String location; // Lima Centro, Pabellón A, etc.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getSeverity() {
        return severity;
    }
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    // Alias para priority compatible con frontend
    public String getPriority() {
        return severity;
    }
    public void setPriority(String priority) {
        this.severity = priority;
    }
    public String getCategory() {
        return category != null && !category.trim().isEmpty() ? category : "General";
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getLocation() {
        return location != null && !location.trim().isEmpty() ? location : "Campus UTP";
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public String getTicketCode() {
        if (id == null) return "INC-NEW";
        return "INC-" + (1000 + id);
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
