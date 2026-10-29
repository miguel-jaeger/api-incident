package com.incident.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.incident.app.dto.RegisterUserRequest;
import com.incident.app.model.User;
import com.incident.app.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "SUPERVISOR", "OPERATOR", "SUPPORT");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository repo;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User register(RegisterUserRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Los datos del usuario son obligatorios.");
        }

        String username = trim(req.getUsername());
        String name = trim(req.getName());
        String email = trim(req.getEmail());
        String password = req.getPassword() != null ? req.getPassword() : "";
        String role = trim(req.getRole()).toUpperCase(Locale.ROOT);

        if (username.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (password.isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("El correo electrónico no tiene un formato válido.");
        }
        if (role.isEmpty()) {
            role = "OPERATOR";
        }
        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Rol inválido. Valores permitidos: ADMIN, SUPERVISOR, OPERATOR, SUPPORT.");
        }
        if (repo.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("El nombre de usuario ya está registrado.");
        }
        if (!email.isEmpty() && repo.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("El correo electrónico ya está registrado.");
        }

        User user = new User();
        user.setUsername(username);
        user.setName(name.isEmpty() ? username : name);
        user.setEmail(email.isEmpty() ? null : email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());

        return repo.save(user);
    }

    public List<User> listAll() {
        return repo.findAll();
    }

    public User findById(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con id " + id));
    }

    public User changeRole(Long id, String role) {
        if (id == null) {
            throw new IllegalArgumentException("El id del usuario es obligatorio.");
        }

        String normalizedRole = trim(role).toUpperCase(Locale.ROOT);
        if (normalizedRole.isEmpty()) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Rol inválido. Valores permitidos: ADMIN, SUPERVISOR, OPERATOR, SUPPORT.");
        }

        User user = findById(id);
        user.setRole(normalizedRole);
        return repo.save(user);
    }

    public User authenticate(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null) {
            return null;
        }

        String identifier = usernameOrEmail.trim();
        Optional<User> user = repo.findByUsernameIgnoreCase(identifier);
        if (user.isEmpty()) {
            user = repo.findByEmailIgnoreCase(identifier);
        }

        return user.filter(u -> passwordEncoder.matches(password, u.getPassword())).orElse(null);
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
