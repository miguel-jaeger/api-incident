package com.incident.app;

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

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/test-user-endpoints.db")
@AutoConfigureMockMvc
class UserManagementEndpointsTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void adminPuedeListarConsultarYCambiarRoles() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");
        String username = "user." + UUID.randomUUID().toString().substring(0, 8);

        MvcResult registered = mockMvc.perform(post("/api/auth/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "username", username,
                    "name", "Usuario de consulta",
                    "email", username + "@utp.edu.pe",
                    "password", "clave123",
                    "role", "OPERATOR"
                ))))
            .andExpect(status().isCreated())
            .andReturn();

        long userId = mapper.readTree(registered.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isForbidden());

        MvcResult list = mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andReturn();

        String listBody = list.getResponse().getContentAsString();
        if (!listBody.contains("\"" + username + "\"")) {
            throw new AssertionError("La lista de usuarios no contiene al usuario de prueba");
        }

        mockMvc.perform(get("/api/auth/users/{id}", userId)
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auth/users/{id}", userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(get("/api/auth/users/{id}", 999999)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/auth/roles")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("userId", userId, "role", "SUPERVISOR"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/auth/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("userId", userId, "role", "ROOT"))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/auth/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("userId", userId, "role", "SUPERVISOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.role").value("SUPERVISOR"));

        // Alias: id + newRole
        mockMvc.perform(patch("/api/auth/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("id", userId, "newRole", "SUPPORT"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("SUPPORT"));

        // Alias: username + role
        mockMvc.perform(patch("/api/auth/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "role", "OPERATOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("OPERATOR"));
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
