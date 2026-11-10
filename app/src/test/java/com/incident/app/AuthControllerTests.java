package com.incident.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pruebas de los endpoints expuestos por AuthController:
 * POST /api/auth/login, GET /api/auth/me y POST /api/auth/logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    private static final String DB_PATH = "target/test-auth-controller-" + UUID.randomUUID() + ".db";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB_PATH);
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * POST /api/auth/login: con usuario y contraseña válidos devuelve 200,
     * un token no vacío y los datos del usuario autenticado.
     */
    @Test
    void loginConCredencialesValidasDevuelveTokenYPerfil() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", "admin", "password", "admin1234"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.name").value("Admin TI UTP"))
            .andExpect(jsonPath("$.email").value("admin@utp.edu.pe"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    /**
     * POST /api/auth/login: el identificador también puede ser el correo
     * electrónico registrado, no solo el nombre de usuario.
     */
    @Test
    void loginPorCorreoElectronicoTambienEsValido() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "admin@utp.edu.pe", "password", "admin123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.username").value("admin"));
    }

    /**
     * POST /api/auth/login: una contraseña incorrecta devuelve 401 y un
     * mensaje de error, sin token de sesión.
     */
    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", "admin", "password", "incorrecta"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.authenticated").value(false))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /**
     * POST /api/auth/login: un usuario inexistente devuelve 401.
     */
    @Test
    void loginConUsuarioInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", "no-existe", "password", "admin123"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.authenticated").value(false));
    }

    /**
     * GET /api/auth/me: con un token válido devuelve la sesión actual
     * (usuario, nombre, correo y rol).
     */
    @Test
    void meDevuelveLaSesionDelToken() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.name").value("Admin TI UTP"))
            .andExpect(jsonPath("$.email").value("admin@utp.edu.pe"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    /**
     * GET /api/auth/me: sin token (o con uno inválido) devuelve 401 y
     * authenticated=false.
     */
    @Test
    void meSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.authenticated").value(false))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /**
     * POST /api/auth/logout: invalida el token recibido; después de cerrar
     * sesión, GET /api/auth/me responde 401 con ese mismo token.
     */
    @Test
    void logoutInvalidaElToken() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    /**
     * POST /api/auth/logout: sin token responde 200 igualmente, porque el
     * cierre de sesión es idempotente.
     */
    @Test
    void logoutSinTokenEsIdempotente() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));
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
