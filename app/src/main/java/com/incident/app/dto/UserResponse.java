package com.incident.app.dto;

import java.time.LocalDateTime;

import com.incident.app.model.User;

public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String role;
    private LocalDateTime createdAt;

    public UserResponse() {}

    public static UserResponse from(User user) {
        UserResponse res = new UserResponse();
        res.id = user.getId();
        res.username = user.getUsername();
        res.name = user.getName();
        res.email = user.getEmail();
        res.role = user.getRole();
        res.createdAt = user.getCreatedAt();
        return res;
    }

    public Long getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public String getRole() {
        return role;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
