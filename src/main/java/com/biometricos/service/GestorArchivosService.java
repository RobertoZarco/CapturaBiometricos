package com.biometricos.service;

import com.biometricos.model.DatosBiometricos;
import com.biometricos.model.HuellaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;


@Service
public class GestorArchivosService {

    private static final String CARPETA_BASE = "datos_biometricos";
    private static final String CARPETA_HUELLAS = "huellas";
    private static final String CARPETA_FOTOS = "fotos";
    private static final String CARPETA_FIRMAS = "firmas";
    private static final String CARPETA_JSON = "registros";

    public GestorArchivosService() {
        crearEstructuraCarpetas();
    }

    private void crearEstructuraCarpetas() {
        crearCarpetaSiNoExiste(CARPETA_BASE);
        crearCarpetaSiNoExiste(CARPETA_BASE + File.separator + CARPETA_HUELLAS);
        crearCarpetaSiNoExiste(CARPETA_BASE + File.separator + CARPETA_FOTOS);
        crearCarpetaSiNoExiste(CARPETA_BASE + File.separator + CARPETA_FIRMAS);
        crearCarpetaSiNoExiste(CARPETA_BASE + File.separator + CARPETA_JSON);
    }

    private void crearCarpetaSiNoExiste(String ruta) {
        File carpeta = new File(ruta);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
    }

    public void guardarDatosCompletos(String referencia, HuellaResponse huella, String imagenHuellaBase64,
                                      String fotoBase64, String firmaBase64) throws IOException {

        // 1. Guardar JSON con metadatos
        guardarJSON(referencia, huella);

        // 2. Guardar huellas en carpeta huellas
        if (imagenHuellaBase64 != null && !imagenHuellaBase64.isEmpty()) {
            guardarImagenHuella(referencia, imagenHuellaBase64);
        }

        // 3. Guardar foto en carpeta fotos
        if (fotoBase64 != null && !fotoBase64.isEmpty()) {
            guardarFoto(referencia, fotoBase64);
        }

        // 4. Guardar firma en carpeta firmas
        if (firmaBase64 != null && !firmaBase64.isEmpty()) {
            guardarFirma(referencia, firmaBase64);
        }
    }

    private void guardarJSON(String referencia, HuellaResponse huella) throws IOException {
        // Crear objeto DatosBiometricos
        DatosBiometricos datos = new DatosBiometricos();
        datos.setReferencia(referencia);
        datos.setFechaCaptura(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        datos.setHuella(huella);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String filename = CARPETA_BASE + File.separator + CARPETA_JSON + File.separator +
                referencia + ".json";

        mapper.writeValue(new File(filename), datos);

        // También guardar versión TXT
        guardarTXT(referencia, huella);
    }

    private void guardarTXT(String referencia, HuellaResponse huella) throws IOException {
        String filename = CARPETA_BASE + File.separator + CARPETA_JSON + File.separator +
                referencia + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("REFERENCIA: " + referencia);
            writer.println("FECHA CAPTURA: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("=== DATOS HUELLA ===");
            writer.println("Calidad: " + (huella.getCalidad() != null ? huella.getCalidad() + "%" : "N/A"));
            writer.println("Minucias: " + (huella.getMinucias() != null ? huella.getMinucias() : "N/A"));
            writer.println("Dedo: " + (huella.getDedo() != null ? huella.getDedo() : "N/A"));
            writer.println("Mano: " + (huella.getMano() != null ? huella.getMano() : "N/A"));
            writer.println("Dispositivo: " + (huella.getDispositivo() != null ? huella.getDispositivo() : "N/A"));
            writer.println("Exitoso: " + huella.isExitoso());
            writer.println("Mensaje: " + (huella.getMensaje() != null ? huella.getMensaje() : "N/A"));
        }
    }

    private void guardarImagenHuella(String referencia, String imagenBase64) throws IOException {
        // Asumiendo que en HuellaResponse tienes información del dedo
        String dedo = "4"; // pulgar izquierdo por defecto
        String mano = "izquierda";

        // Aquí deberías obtener esta información de HuellaResponse
        // Por ahora usaremos un nombre genérico
        String filename = CARPETA_BASE + File.separator + CARPETA_HUELLAS + File.separator +
                referencia + "_huella.jpg";

        guardarImagenBase64(imagenBase64, filename);
    }

    private void guardarFoto(String referencia, String fotoBase64) throws IOException {
        String filename = CARPETA_BASE + File.separator + CARPETA_FOTOS + File.separator +
                referencia + ".jpg";

        guardarImagenBase64(fotoBase64, filename);
    }

    private void guardarFirma(String referencia, String firmaBase64) throws IOException {
        String filename = CARPETA_BASE + File.separator + CARPETA_FIRMAS + File.separator +
                referencia + ".jpg";

        guardarImagenBase64(firmaBase64, filename);
    }

    private void guardarImagenBase64(String base64, String filePath) throws IOException {
        try {
            // Remover el prefijo si existe (data:image/jpeg;base64,)
            String base64Data = base64;
            if (base64.contains(",")) {
                base64Data = base64.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            Files.write(Paths.get(filePath), imageBytes);
        } catch (Exception e) {
            throw new IOException("Error al decodificar imagen Base64: " + e.getMessage());
        }
    }

    public String obtenerEstructuraCarpetas() {
        return "Estructura de carpetas:\n" +
                CARPETA_BASE + "/\n" +
                "├── " + CARPETA_HUELLAS + "/\n" +
                "├── " + CARPETA_FOTOS + "/\n" +
                "├── " + CARPETA_FIRMAS + "/\n" +
                "└── " + CARPETA_JSON + "/";
    }
}