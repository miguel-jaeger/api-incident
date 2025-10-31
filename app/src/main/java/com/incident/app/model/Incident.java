package com.incident.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;


@Entity
public class Incident {
    @Id @GeneratedValue
    private Long id;
    private String title;
    private String description;
    private String status; // por ejemplo: OPEN, IN_PROGRESS, RESOLVED, CLOSED
    private String severity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // getters/setters
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
}
