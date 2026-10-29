package com.incident.app.dto;

public class RegisterUserRequest {
    private String username;
    private String name;
    private String fullName;
    private String email;
    private String password;
    private String role;

    public RegisterUserRequest() {}

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getName() {
        return name != null ? name : fullName;
    }
    public void setName(String name) {
        this.name = name;
    }
    // Alias compatible con clientes que envían fullName
    public String getFullName() {
        return fullName != null ? fullName : name;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
}
