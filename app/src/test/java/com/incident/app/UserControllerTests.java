package com.incident.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Pruebas de los endpoints expuestos por UserController:
 * GET /api/users, GET /api/users/{id}, POST /api/users,
 * PATCH /api/users/{id}/role, PATCH /api/users/{id}/password y
 * DELETE /api/users/{id}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTests {

    private static final String DB_PATH = "target/test-user-controller-" + UUID.randomUUID() + ".db";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB_PATH);
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * POST /api/users: el registro público crea un OPERATOR por defecto y la
     * respuesta nunca expone la contraseña.
     */
    @Test
    void registroPublicoCreaUsuarioOperatorPorDefecto() throws Exception {
        String username = uniqueUsername("publico");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "username", username,
                    "name", "Usuario público",
                    "email", username + "@utp.edu.pe",
                    "password", "clave123"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.username").value(username))
            .andExpect(jsonPath("$.role").value("OPERATOR"))
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    /**
     * POST /api/users: sin sesión de ADMIN no se pueden asignar roles
     * distintos de OPERATOR (por ejemplo ADMIN o SUPERVISOR).
     */
    @Test
    void registroPublicoNoPuedeElevarPrivilegios() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "username", uniqueUsername("intento.admin"),
                    "name", "Intento admin",
                    "email", uniqueUsername("intento.admin") + "@utp.edu.pe",
                    "password", "clave123",
                    "role", "ADMIN"
                ))))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "username", uniqueUsername("intento.supervisor"),
                    "name", "Intento supervisor",
                    "email", uniqueUsername("intento.supervisor") + "@utp.edu.pe",
                    "password", "clave123",
                    "role", "SUPERVISOR"
                ))))
            .andExpect(status().isForbidden());
    }

    /**
     * POST /api/users: un ADMIN autenticado sí puede crear usuarios con
     * cualquier rol permitido.
     */
    @Test
    void adminPuedeCrearUsuarioConRolEspecifico() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = uniqueUsername("supervisor");

        createUser(username, "clave123", "SUPERVISOR", adminToken)
            .andExpect(jsonPath("$.username").value(username))
            .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    /**
     * POST /api/users: valida la longitud de la contraseña y rechaza
     * username o email duplicados con 400 y 409 respectivamente.
     */
    @Test
    void registroValidaDatosYDuplicados() throws Exception {
        String username = uniqueUsername("validacion");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "username", username,
                    "name", "Clave corta",
                    "email", username + "@utp.edu.pe",
                    "password", "123"
                ))))
            .andExpect(status().isBadRequest());

        createUser(username, "clave123", "OPERATOR", null);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "username", username,
                    "name", "Usuario duplicado",
                    "email", uniqueUsername("otro") + "@utp.edu.pe",
                    "password", "clave123"
                ))))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "username", uniqueUsername("correo.duplicado"),
                    "name", "Correo duplicado",
                    "email", username + "@utp.edu.pe",
                    "password", "clave123"
                ))))
            .andExpect(status().isConflict());
    }

    /**
     * GET /api/users: exige sesión (401) y rol ADMIN (403); con ADMIN
     * devuelve el listado e incluye a los usuarios creados.
     */
    @Test
    void listarUsuariosRequiereAdmin() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");
        String username = uniqueUsername("listado");
        createUser(username, "clave123", "OPERATOR", null);

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isForbidden());

        MvcResult result = mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains(username));
    }

    /**
     * GET /api/users/{id}: exige ADMIN, devuelve el usuario existente y
     * responde 404 cuando el id no existe.
     */
    @Test
    void obtenerUsuarioPorIdRequiereAdmin() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");
        String username = uniqueUsername("consulta");
        long userId = userId(createUser(username, "clave123", "OPERATOR", null));

        mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/{id}", userId)
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{id}", userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(get("/api/users/{id}", 999999)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    /**
     * PATCH /api/users/{id}/role: solo ADMIN puede cambiar roles, se valida
     * que el rol exista y el cambio se refleja en la respuesta.
     */
    @Test
    void soloAdminPuedeCambiarRolYSeValidaElValor() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");
        long userId = userId(createUser(uniqueUsername("rol"), "clave123", "OPERATOR", null));

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("role", "SUPERVISOR"))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("role", "SUPERVISOR"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("role", "ROOT"))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("role", "SUPERVISOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    /**
     * PATCH /api/users/{id}/password: el propio usuario debe indicar su
     * contraseña actual; un ADMIN puede cambiar la de otro usuario sin ella.
     * También se comprueba que la contraseña antigua deja de funcionar.
     */
    @Test
    void usuarioCambiaSuPasswordYAdminPuedeCambiarlaSinActual() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = uniqueUsername("password");
        long userId = userId(createUser(username, "clave123", "OPERATOR", null));
        String userToken = login(username, "clave123");

        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", "clave123", "newPassword", "nueva123"))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/users/{id}/password", 999999)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", "x", "newPassword", "nueva123"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", "incorrecta", "newPassword", "nueva123"))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("newPassword", "nueva123"))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", "clave123", "newPassword", "nueva123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId));

        assertEquals(401, loginStatus(username, "clave123"));
        login(username, "nueva123");

        mockMvc.perform(patch("/api/users/{id}/password", userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("newPassword", "adminClave123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId));

        assertEquals(401, loginStatus(username, "nueva123"));
        login(username, "adminClave123");
    }

    /**
     * DELETE /api/users/{id}: exige ADMIN, devuelve 204 al eliminar, el
     * usuario deja de existir y no puede volver a iniciar sesión.
     */
    @Test
    void soloAdminPuedeEliminarUsuario() throws Exception {
        String adminToken = login("admin", "admin123");
        String operatorToken = login("operador", "operador123");
        String username = uniqueUsername("eliminar");
        long userId = userId(createUser(username, "clave123", "OPERATOR", null));

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

    private org.springframework.test.web.servlet.ResultActions createUser(
            String username,
            String password,
            String role,
            String token) throws Exception {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("name", "Usuario de prueba");
        body.put("email", username + "@utp.edu.pe");
        body.put("password", password);
        if (role != null) {
            body.put("role", role);
        }

        var request = post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(body));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        return mockMvc.perform(request)
            .andExpect(status().isCreated());
    }

    private long userId(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        MvcResult mvcResult = result.andReturn();
        return mapper.readTree(mvcResult.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();

        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private int loginStatus(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", password))))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private String uniqueUsername(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8);
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }
}
