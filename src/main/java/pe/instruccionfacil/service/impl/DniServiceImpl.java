package pe.instruccionfacil.service.impl;

import pe.instruccionfacil.service.service.*;
import pe.instruccionfacil.exception.ApiException;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

@Service
public class DniServiceImpl implements DniService {
    private final RestClient http;
    private final String base;
    private final String token;
    private final boolean obligatorio;

    public DniServiceImpl(RestClient http, @Value("${app.dni-api-url}") String base,
                      @Value("${app.dni-token}") String token,
                      @Value("${app.dni-obligatorio}") boolean obligatorio) {
        this.http = http;
        this.base = base;
        this.token = token;
        this.obligatorio = obligatorio;
    }

    public void verificar(String dni, String nombreCompleto) {
        if (token.isBlank()) {
            if (obligatorio) throw noDisponible();
            return;
        }
        if (dni == null || !dni.matches("[0-9]{8}")) {
            throw ApiException.invalido("El DNI debe contener ocho dígitos");
        }
        JsonNode respuesta;
        try {
            respuesta = http.get().uri(base + "/v1/dni/{dni}", dni)
                .headers(headers -> headers.setBearerAuth(token)).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw datosIncorrectos();
            throw noDisponible();
        } catch (RestClientException ex) {
            throw noDisponible();
        }
        if (respuesta == null || !respuesta.path("success").asBoolean()) throw noDisponible();
        var datos = respuesta.path("data");
        String oficial = normalizar(datos.path("nombre_completo").asString(""));
        if (!dni.equals(datos.path("dni").asString("")) || oficial.isEmpty()) throw noDisponible();
        if (!oficial.equals(normalizar(nombreCompleto))) throw datosIncorrectos();
    }

    private static String normalizar(String nombre) {
        return Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT).replaceAll("[^\\p{L}]+", " ").trim();
    }

    private static ApiException datosIncorrectos() {
        return ApiException.invalido("Verifica que el DNI y el nombre completo coincidan con el documento");
    }

    private static ApiException noDisponible() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "La verificación de DNI no está disponible en este momento");
    }
}
