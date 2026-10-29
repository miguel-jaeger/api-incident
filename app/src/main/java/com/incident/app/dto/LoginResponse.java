package com.incident.app.dto;

public class LoginResponse {
    private boolean authenticated;
    private String token;
    private String username;
    private String name;
    private String email;
    private String role;
    private String message;

    public LoginResponse() {}

    public LoginResponse(boolean authenticated, String token, String username, String name, String email, String role, String message) {
        this.authenticated = authenticated;
        this.token = token;
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
        this.message = message;
    }

    public static LoginResponse success(String token, String username, String name, String email, String role) {
        return new LoginResponse(true, token, username, name, email, role, "Autenticación exitosa");
    }

    public static LoginResponse failure(String message) {
        return new LoginResponse(false, null, null, null, null, null, message);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
