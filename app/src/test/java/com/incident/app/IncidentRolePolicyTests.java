package com.incident.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/test-role-policy.db")
@AutoConfigureMockMvc
class IncidentRolePolicyTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void usuarioAutenticadoPuedeCrearPeroSoloAdminPuedeCambiarEstado() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");

        mockMvc.perform(post("/api/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Sin sesión\",\"description\":\"x\",\"severity\":\"Low\"}"))
            .andExpect(status().isUnauthorized());

        MvcResult created = mockMvc.perform(post("/api/incidents")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Incidente de operador\",\"description\":\"x\",\"severity\":\"Medium\",\"status\":\"Inactive\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Active"))
            .andReturn();

        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/incidents/{id}/status", id)
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"Inactive\",\"message\":\"No permitido\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/incidents/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"Inactive\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/incidents/{id}/status", id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"Inactive\",\"message\":\"Cerrado por administrador\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Inactive"))
            .andExpect(jsonPath("$.message").value("Cerrado por administrador"));

        mockMvc.perform(patch("/api/incidents/{id}/priority", id)
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"priority\":\"High\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/incidents/{id}/priority", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"priority\":\"High\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/incidents/{id}/priority", id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"priority\":\"High\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value("High"))
            .andExpect(jsonPath("$.severity").value("High"));
    }

    @Test
    void prioridadPorDefectoEsLow() throws Exception {
        String operatorToken = login("operador", "operador123");

        MvcResult created = mockMvc.perform(post("/api/incidents")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Sin prioridad\",\"description\":\"x\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.priority").value("Low"))
            .andExpect(jsonPath("$.severity").value("Low"))
            .andReturn();

        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(delete("/api/incidents/{id}", id))
            .andExpect(status().isNoContent());
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
