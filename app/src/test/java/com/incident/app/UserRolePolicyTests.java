package com.incident.app;

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

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/test-user-role.db")
@AutoConfigureMockMvc
class UserRolePolicyTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void soloAdminPuedeCambiarRolYElCambioAplicaImediatamente() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = "role." + UUID.randomUUID().toString().substring(0, 8);

        // El registro público no puede crear administradores
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "username", username,
                    "name", "Intento admin",
                    "email", username + "@utp.edu.pe",
                    "password", "clave123",
                    "role", "ADMIN"
                ))))
            .andExpect(status().isForbidden());

        MvcResult registered = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "username", username,
                    "name", "Usuario de prueba",
                    "email", username + "@utp.edu.pe",
                    "password", "clave123",
                    "role", "OPERATOR"
                ))))
            .andExpect(status().isCreated())
            .andReturn();

        long userId = mapper.readTree(registered.getResponse().getContentAsString()).get("id").asLong();
        String userToken = login(username, "clave123");

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"SUPPORT\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"SUPPORT\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ROOT\"}"))
            .andExpect(status().isBadRequest());

        MvcResult created = mockMvc.perform(post("/api/incidents")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Incidente para prueba de rol\",\"description\":\"x\",\"severity\":\"Low\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        long incidentId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.role").value("ADMIN"));

        // El token emitido antes del cambio ya tiene permisos de ADMIN
        mockMvc.perform(patch("/api/incidents/{id}/status", incidentId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"Inactive\",\"message\":\"Actualizado tras cambio de rol\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Inactive"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();

        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
