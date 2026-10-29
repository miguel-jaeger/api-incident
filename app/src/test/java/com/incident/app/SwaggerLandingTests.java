package com.incident.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:sqlite:target/test-swagger-landing.db"
)
class SwaggerLandingTests {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    @Test
    void laRaizSirveSwaggerSinRedireccion() throws Exception {
        HttpResponse<String> response = get("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("text/html"));
        assertTrue(response.body().contains("<base href=\"/swagger-ui/\">"));
        assertTrue(response.body().contains("url: \"/openapi.json\""));
    }

    @Test
    void docsMantieneLaUrlYSirveLaPaginaDeSwagger() throws Exception {
        HttpResponse<String> response = get("/docs");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<base href=\"/swagger-ui/\">"));
    }

    @Test
    void lasRutasInternasDeSwaggerTerminanEnLaRaiz() throws Exception {
        HttpClient followingClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        HttpResponse<String> indexResponse = followingClient.send(
            HttpRequest.newBuilder(URI.create(baseUrl() + "/swagger-ui/index.html#/")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, indexResponse.statusCode());
        assertEquals("/", indexResponse.uri().getPath());

        HttpResponse<String> htmlResponse = followingClient.send(
            HttpRequest.newBuilder(URI.create(baseUrl() + "/swagger-ui.html")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, htmlResponse.statusCode());
        assertEquals("/", htmlResponse.uri().getPath());
    }

    @Test
    void losRecursosDeSwaggerSiguenDisponibles() throws Exception {
        HttpResponse<String> response = get("/swagger-ui/swagger-ui.css");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("text/css"));
    }

    @Test
    void elContratoOpenApiEstaEnUnaRutaClara() throws Exception {
        HttpResponse<String> response = get("/openapi.json");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/json"));
        assertTrue(response.body().contains("\"openapi\""));
        assertTrue(response.body().contains("\"/api/users\""));
    }

    @Test
    void laRutaAntiguaDelContratoOpenApiRedirige() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");

        assertEquals(302, response.statusCode());
        String location = response.headers().firstValue("location").orElse("");
        assertEquals("/openapi.json", URI.create(location).getPath());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
