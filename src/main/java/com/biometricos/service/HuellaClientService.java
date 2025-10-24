package com.biometricos.service;

import com.biometricos.model.CapturaRequest;
import com.biometricos.model.ConfiguracionCaptura;
import com.biometricos.model.HuellaResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Collections;

@Service
public class HuellaClientService {

    @Value("${huella.service.url:http://localhost:8081}")
    private String huellaServiceUrl;

    private final RestTemplate restTemplate;

    public HuellaClientService() {
        this.restTemplate = new RestTemplate();
        // Configurar timeout
        // ((HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory()).setReadTimeout(30000);
    }

    public HuellaResponse capturarHuella(String dispositivo) {
        String url = huellaServiceUrl + "/api/huellas/capturar";

        try {
            CapturaRequest capturaRequest = new CapturaRequest(dispositivo);
            capturaRequest.setConfiguracion(new ConfiguracionCaptura(512, 512));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CapturaRequest> request = new HttpEntity<>(capturaRequest, headers);

            System.out.println("🔗 Conectando con: " + url);

            // Configurar ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Realizar la llamada
            ResponseEntity<String> responseRaw = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class
            );

            HuellaResponse huellaResponse = mapper.readValue(responseRaw.getBody(), HuellaResponse.class);

            System.out.println("✅ Respuesta parseada correctamente");
            System.out.println("✅ Éxito: " + huellaResponse.isExitoso());
            System.out.println("📊 Calidad: " + huellaResponse.getCalidad());
            System.out.println("📊 Calidad Imagen: '" + huellaResponse.getCalidadImagen() + "'");
            System.out.println("🔢 Minucias: " + huellaResponse.getMinuciasFormateadas());

            // Log detallado de imágenes
            huellaResponse.logInfoImagenes();

            return huellaResponse;

        } catch (Exception e) {
            System.out.println("❌ Error en captura: " + e.getMessage());
            e.printStackTrace();
            return crearRespuestaError("Error: " + e.getMessage());
        }
    }

    public boolean verificarConexion() {
        try {
            // Probar con un endpoint simple o el mismo de captura
            String url = huellaServiceUrl + "/api/huellas/capturar";
            CapturaRequest testRequest = new CapturaRequest("TEST");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CapturaRequest> request = new HttpEntity<>(testRequest, headers);

            // Hacer una llamada rápida para verificar
            restTemplate.exchange(url, HttpMethod.POST, request, HuellaResponse.class);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Microservicio no disponible: " + e.getMessage());
            return false;
        }
    }

    private HuellaResponse crearRespuestaError(String mensaje) {
        HuellaResponse response = new HuellaResponse();
        response.setExitoso(false);
        response.setMensaje(mensaje);
        return response;
    }


    private void agregarLog(String mensaje) {
        // Puedes inyectar un logger o usar System.out para debugging
        System.out.println("[HuellaClient] " + mensaje);
    }

    public void debugRespuestaCompleta(String dispositivo) {
        try {
            String url = huellaServiceUrl + "/api/huellas/capturar";
            CapturaRequest capturaRequest = new CapturaRequest(dispositivo);
            capturaRequest.setConfiguracion(new ConfiguracionCaptura(512, 512));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CapturaRequest> request = new HttpEntity<>(capturaRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class
            );

            System.out.println("=== RESPUESTA JSON COMPLETA ===");
            String jsonResponse = response.getBody();

            // Mostrar solo las primeras 2000 caracteres para no saturar la consola
            if (jsonResponse.length() > 2000) {
                System.out.println(jsonResponse.substring(0, 2000) + "... [TRUNCADO]");
            } else {
                System.out.println(jsonResponse);
            }

            // Buscar específicamente campos de imagen
            if (jsonResponse.contains("ImagenWsq")) {
                int start = jsonResponse.indexOf("ImagenWsq");
                int end = jsonResponse.indexOf("\"", start + 12); // Buscar después del valor
                System.out.println("🔍 Campo ImagenWsq encontrado alrededor de: " +
                        jsonResponse.substring(start, Math.min(end + 10, jsonResponse.length())));
            }

            if (jsonResponse.contains("imagenBmp")) {
                int start = jsonResponse.indexOf("imagenBmp");
                int end = jsonResponse.indexOf("\"", start + 12);
                System.out.println("🔍 Campo imagenBmp encontrado alrededor de: " +
                        jsonResponse.substring(start, Math.min(end + 10, jsonResponse.length())));
            }

            System.out.println("=====================================");

        } catch (Exception e) {
            System.out.println("Error en debug: " + e.getMessage());
        }
    }
}