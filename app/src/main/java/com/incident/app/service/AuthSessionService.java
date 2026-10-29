package com.incident.app.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

import com.incident.app.model.User;

/**
 * Mantiene las sesiones activas en memoria. Cada token emitido en el login
 * queda asociado al usuario de la base de datos.
 */
@Service
public class AuthSessionService {

    private final ConcurrentMap<String, User> sessions = new ConcurrentHashMap<>();

    public String createSession(User user) {
        String token = "utp-token-" + UUID.randomUUID();
        sessions.put(token, user);
        return token;
    }

    public Optional<User> findByAuthorizationHeader(String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    public void invalidate(String authHeader) {
        String token = extractToken(authHeader);
        if (token != null) {
            sessions.remove(token);
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        String token = authHeader.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        return token.isEmpty() ? null : token;
    }
}
