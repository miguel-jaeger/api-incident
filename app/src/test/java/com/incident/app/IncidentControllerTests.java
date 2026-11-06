package com.incident.app;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.app.repository.IncidentRepository;

/**
 * Pruebas de los endpoints expuestos por IncidentController:
 * POST /api/incidents, PATCH /api/incidents/{id}/status,
 * PATCH /api/incidents/{id}/priority, GET /api/incidents,
 * GET /api/incidents/{id}, DELETE /api/incidents/{id} y los endpoints
 * de estadísticas bajo /api/incidents/stats.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentControllerTests {

    private static final String DB_PATH = "target/test-incident-controller-" + UUID.randomUUID() + ".db";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB_PATH);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Deja la base de datos sin incidentes antes de cada prueba para que los
     * listados y las estadísticas tengan valores deterministas.
     */
    @BeforeEach
    void limpiarIncidentes() {
        incidentRepository.deleteAll();
    }

    /**
     * POST /api/incidents: sin cabecera Authorization devuelve 401 y no crea
     * el incidente.
     */
    @Test
    void crearIncidenteSinSesionDevuelve401() throws Exception {
        mockMvc.perform(post("/api/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "title", "Incidente sin sesión",
                    "description", "No debería crearse"
                ))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /**
     * POST /api/incidents: un usuario autenticado puede publicar; si no envía
     * prioridad, categoría, ubicación ni estado, se aplican los valores por
     * defecto Low, General, Campus UTP y Active.
     */
    @Test
    void crearIncidenteConSesionAplicaValoresPorDefecto() throws Exception {
        String operatorToken = login("operador", "operador123");

        mockMvc.perform(post("/api/incidents")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "title", "Incidente con valores por defecto",
                    "description", "Descripción de prueba"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.title").value("Incidente con valores por defecto"))
            .andExpect(jsonPath("$.status").value("Active"))
            .andExpect(jsonPath("$.priority").value("Low"))
            .andExpect(jsonPath("$.severity").value("Low"))
            .andExpect(jsonPath("$.category").value("General"))
            .andExpect(jsonPath("$.location").value("Campus UTP"))
            .andExpect(jsonPath("$.ticketCode").isNotEmpty());
    }

    /**
     * POST /api/incidents: solo ADMIN puede definir el estado inicial; un
     * OPERATOR que envíe status=Inactive recibe un incidente en Active.
     */
    @Test
    void adminPuedeDefinirEstadoInicialYOperadorNo() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");

        mockMvc.perform(post("/api/incidents")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "title", "Creado por admin",
                    "description", "Estado inicial controlado",
                    "status", "Inactive"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Inactive"));

        mockMvc.perform(post("/api/incidents")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "title", "Creado por operador",
                    "description", "El estado debe ignorarse",
                    "status", "Inactive"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Active"));
    }

    /**
     * PATCH /api/incidents/{id}/status: exige sesión y rol ADMIN, valida que
     * status no esté vacío y actualiza el estado y el mensaje.
     */
    @Test
    void actualizarEstadoValidaSesionRolYCampo() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");
        long incidentId = createIncident(operatorToken, "Incidente para estado", null, null, null);

        mockMvc.perform(patch("/api/incidents/{id}/status", incidentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "Inactive"))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/incidents/{id}/status", incidentId)
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "Inactive"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/incidents/{id}/status", incidentId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/incidents/{id}/status", incidentId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "Inactive", "message", "Cerrado por administrador"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Inactive"))
            .andExpect(jsonPath("$.message").value("Cerrado por administrador"));
    }

    /**
     * PATCH /api/incidents/{id}/priority: exige sesión y rol ADMIN, valida que
     * la prioridad no esté vacía y actualiza priority/severity.
     */
    @Test
    void actualizarPrioridadValidaSesionRolYCampo() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");
        long incidentId = createIncident(operatorToken, "Incidente para prioridad", null, null, null);

        mockMvc.perform(patch("/api/incidents/{id}/priority", incidentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("priority", "High"))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/incidents/{id}/priority", incidentId)
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("priority", "High"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/incidents/{id}/priority", incidentId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/incidents/{id}/priority", incidentId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("priority", "High"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value("High"))
            .andExpect(jsonPath("$.severity").value("High"));

        mockMvc.perform(patch("/api/incidents/{id}/priority", incidentId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("severity", "Medium"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value("Medium"))
            .andExpect(jsonPath("$.severity").value("Medium"));
    }

    /**
     * POST /api/incidents: acepta los alias priority y rejectionReason del
     * frontend, mapeándolos a severity y message.
     */
    @Test
    void crearIncidenteAceptaAliasPriorityYRejectionReason() throws Exception {
        String operatorToken = login("operador", "operador123");

        mockMvc.perform(post("/api/incidents")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "title", "Incidente con alias",
                    "description", "Descripción de prueba",
                    "priority", "Medium",
                    "rejectionReason", "Motivo reportado"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.priority").value("Medium"))
            .andExpect(jsonPath("$.severity").value("Medium"))
            .andExpect(jsonPath("$.message").value("Motivo reportado"));
    }

    /**
     * GET /api/incidents y GET /api/incidents/{id}: devuelven el listado y el
     * detalle del incidente creado, sin requerir autenticación.
     */
    @Test
    void listarYConsultarIncidentes() throws Exception {
        String operatorToken = login("operador", "operador123");
        long firstId = createIncident(operatorToken, "Incidente Hardware", "Hardware", "High", null);
        createIncident(operatorToken, "Incidente Redes", "Redes", "Low", null);

        MvcResult list = mockMvc.perform(get("/api/incidents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andReturn();

        String listBody = list.getResponse().getContentAsString();
        assertTrue(listBody.contains("Incidente Hardware"));
        assertTrue(listBody.contains("Incidente Redes"));

        mockMvc.perform(get("/api/incidents/{id}", firstId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firstId))
            .andExpect(jsonPath("$.title").value("Incidente Hardware"))
            .andExpect(jsonPath("$.category").value("Hardware"))
            .andExpect(jsonPath("$.priority").value("High"));
    }

    /**
     * DELETE /api/incidents/{id}: elimina el incidente y devuelve 204; el
     * listado y el contador total quedan vacíos.
     */
    @Test
    void eliminarIncidenteDevuelve204() throws Exception {
        String operatorToken = login("operador", "operador123");
        long incidentId = createIncident(operatorToken, "Incidente a eliminar", null, null, null);

        mockMvc.perform(delete("/api/incidents/{id}", incidentId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/incidents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/incidents/stats/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0));
    }

    /**
     * GET /api/incidents/stats/count, /stats/total, /stats/by-category,
     * /stats/by-severity, /stats/by-status y /stats: agrupan correctamente
     * los incidentes por total, categoría, severidad y estado.
     */
    @Test
    void estadisticasAgrupanLosIncidentes() throws Exception {
        String adminToken = login("admin", "admin123");
        createIncident(adminToken, "Hardware activo 1", "Hardware", "High", "Active");
        createIncident(adminToken, "Redes activo 2", "Redes", "High", "Active");
        createIncident(adminToken, "Hardware inactivo 3", "Hardware", "Low", "Inactive");

        mockMvc.perform(get("/api/incidents/stats/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3));

        mockMvc.perform(get("/api/incidents/stats/total"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3));

        mockMvc.perform(get("/api/incidents/stats/by-category"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Hardware").value(2))
            .andExpect(jsonPath("$.Redes").value(1));

        mockMvc.perform(get("/api/incidents/stats/by-severity"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.High").value(2))
            .andExpect(jsonPath("$.Low").value(1));

        mockMvc.perform(get("/api/incidents/stats/by-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Active").value(2))
            .andExpect(jsonPath("$.Inactive").value(1));

        mockMvc.perform(get("/api/incidents/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.active").value(2))
            .andExpect(jsonPath("$.inactive").value(1))
            .andExpect(jsonPath("$.byCategory.Hardware").value(2))
            .andExpect(jsonPath("$.byCategory.Redes").value(1))
            .andExpect(jsonPath("$.bySeverity.High").value(2))
            .andExpect(jsonPath("$.bySeverity.Low").value(1))
            .andExpect(jsonPath("$.byStatus.Active").value(2))
            .andExpect(jsonPath("$.byStatus.Inactive").value(1));
    }

    private long createIncident(
            String token,
            String title,
            String category,
            String severity,
            String status) throws Exception {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "Descripción de " + title);
        if (category != null) {
            body.put("category", category);
        }
        if (severity != null) {
            body.put("severity", severity);
        }
        if (status != null) {
            body.put("status", status);
        }

        var request = post("/api/incidents")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(body));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andReturn();

        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();

        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }
}
