package com.incident.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/test-user-password-delete.db")
@AutoConfigureMockMvc
class UserPasswordDeleteTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void usuarioCambiaSuPasswordYAdminPuedeCambiarLaDeCualquiera() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = "pwd." + UUID.randomUUID().toString().substring(0, 8);

        long userId = createUser(username, "clave123");
        String userToken = login(username, "clave123");

        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"clave123\",\"newPassword\":\"nueva123\"}"))
            .andExpect(status().isUnauthorized());

        // Un usuario normal no puede cambiar la contraseña de otro
        mockMvc.perform(patch("/api/users/{id}/password", 999999)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"x\",\"newPassword\":\"nueva123\"}"))
            .andExpect(status().isForbidden());

        // Contraseña actual incorrecta
        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"incorrecta\",\"newPassword\":\"nueva123\"}"))
            .andExpect(status().isBadRequest());

        // Falta la contraseña actual
        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"nueva123\"}"))
            .andExpect(status().isBadRequest());

        // El usuario cambia su propia contraseña
        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"clave123\",\"newPassword\":\"nueva123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId));

        assertEquals(401, loginStatus(username, "clave123"));
        login(username, "nueva123");

        // El administrador cambia la contraseña de otro usuario sin pedir la actual
        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"adminClave123\"}"))
            .andExpect(status().isOk());

        assertEquals(401, loginStatus(username, "nueva123"));
        login(username, "adminClave123");
    }

    @Test
    void soloAdminPuedeEliminarUsuarios() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");
        String username = "del." + UUID.randomUUID().toString().substring(0, 8);

        long userId = createUser(username, "clave123");

        mockMvc.perform(delete("/api/users/{id}", userId))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/users/{id}", userId)
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/users/{id}", userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());

        assertEquals(401, loginStatus(username, "clave123"));

        mockMvc.perform(delete("/api/users/{id}", 999999)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    private long createUser(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "username", username,
                    "name", "Usuario de prueba",
                    "email", username + "@utp.edu.pe",
                    "password", password,
                    "role", "OPERATOR"
                ))))
            .andExpect(status().isCreated())
            .andReturn();

        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();

        String token = mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertFalse(token.isBlank());
        return token;
    }

    private int loginStatus(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", password))))
            .andReturn()
            .getResponse()
            .getStatus();
    }
}
