package com.incident.app.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

import com.incident.app.model.User;
import com.incident.app.repository.UserRepository;

/**
 * Mantiene las sesiones activas en memoria. Cada token emitido en el login
 * queda asociado al id del usuario; el usuario se consulta desde SQLite en
 * cada petición para reflejar cambios de rol de inmediato.
 */
@Service
public class AuthSessionService {

    private final ConcurrentMap<String, Long> sessions = new ConcurrentHashMap<>();
    private final UserRepository userRepository;

    public AuthSessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String createSession(User user) {
        String token = "utp-token-" + UUID.randomUUID();
        sessions.put(token, user.getId());
        return token;
    }

    public Optional<User> findByAuthorizationHeader(String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return Optional.empty();
        }

        Long userId = sessions.get(token);
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
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
