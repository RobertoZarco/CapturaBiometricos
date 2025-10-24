package com.biometricos.service;

import com.biometricos.model.DatosBiometricos;
import com.biometricos.model.HuellaResponse;
import com.biometricos.util.MapeadorDedos;
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
import java.util.*;


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


    private void crearCarpetaSiNoExiste(String ruta) {
        File carpeta = new File(ruta);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
    }

    public void guardarDatosCompletos(String referencia, Map<Integer, HuellaResponse> huellas,
                                      String fotoBase64, String firmaBase64) throws IOException {

        System.out.println("📁 INICIANDO GUARDADO MULTIPLE");
        System.out.println("   Referencia: " + referencia);
        System.out.println("   Huellas a guardar: " + huellas.size() + " dedo(s)");

        // 1. Crear estructura de carpetas
        crearEstructuraCarpetas();

        // 2. Guardar JSON con metadatos de todas las huellas
        guardarJSONCompleto(referencia, huellas);

        // 3. Guardar imágenes de huella por cada dedo
        for (Map.Entry<Integer, HuellaResponse> entry : huellas.entrySet()) {
            int codigoDedo = entry.getKey();
            HuellaResponse huella = entry.getValue();

            guardarImagenesPorDedo(referencia, codigoDedo, huella);
        }

        // 4. Guardar foto si está disponible
        if (fotoBase64 != null && !fotoBase64.isEmpty()) {
            guardarFoto(referencia, fotoBase64);
        }

        // 5. Guardar firma si está disponible
        if (firmaBase64 != null && !firmaBase64.isEmpty()) {
            guardarFirma(referencia, firmaBase64);
        }

        System.out.println("✅ GUARDADO MULTIPLE COMPLETADO - " + huellas.size() + " huella(s) guardadas");
    }
    
    private void guardarJSON(String referencia, HuellaResponse huella) throws IOException {
        // Crear objeto con todos los datos
        Map<String, Object> datos = new HashMap<>();
        datos.put("referencia", referencia);
        datos.put("fechaCaptura", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        datos.put("dispositivo", huella.getDispositivo());
        datos.put("exitoso", huella.isExitoso());
        datos.put("mensaje", huella.getMensaje());
        datos.put("calidad", huella.getCalidad());
        datos.put("calidadImagen", huella.getCalidadImagen());
        datos.put("calidadTotal", huella.getCalidadTotal());
        datos.put("descripcionCalidad", huella.getDescripcionCalidad());
        datos.put("minucias", huella.getMinucias());
        datos.put("conteoMinucias", huella.getConteoMinucias());
        datos.put("dedo", huella.getDedo());
        datos.put("mano", huella.getMano());
        datos.put("tieneImagenBmp", huella.getImagenBmp() != null);
        datos.put("tieneImagenWsq", huella.getImagenWsq() != null);
        datos.put("tamanoImagenBmp", huella.getImagenBmp() != null ? huella.getImagenBmp().length() : 0);
        datos.put("tamanoImagenWsq", huella.getImagenWsq() != null ? huella.getImagenWsq().length() : 0);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        String filename = CARPETA_BASE + File.separator + CARPETA_JSON + File.separator +
                referencia + ".json";

        mapper.writeValue(new File(filename), datos);
        System.out.println("📄 JSON guardado: " + filename);

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

    private void guardarImagenesHuella(String referencia, HuellaResponse huella) throws IOException {
        // Guardar imagen WSQ si está disponible y es real
        if (huella.tieneImagenWsqReal()) {
            guardarImagenHuella(referencia, huella.getImagenWsq(), "wsq");
            System.out.println("✅ Imagen WSQ guardada: " + referencia + "_huella.wsq (" +
                    huella.getImagenWsq().length() + " caracteres)");
        } else {
            System.out.println("⚠️ No hay imagen WSQ real para guardar");
        }

        // Guardar imagen BMP si está disponible y es real
        if (huella.tieneImagenBmpReal()) {
            guardarImagenHuella(referencia, huella.getImagenBmp(), "bmp");
            System.out.println("✅ Imagen BMP guardada: " + referencia + "_huella.bmp (" +
                    huella.getImagenBmp().length() + " caracteres)");
        } else {
            System.out.println("⚠️ No hay imagen BMP real para guardar");
        }
    }

    private void guardarImagenHuella(String referencia, String imagenBase64, String extension) throws IOException {
        String filename = CARPETA_BASE + File.separator + CARPETA_HUELLAS + File.separator +
                referencia + "_huella." + extension;

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
            // Verificar que los datos base64 no estén vacíos
            if (base64 == null || base64.trim().isEmpty()) {
                System.out.println("❌ Datos base64 vacíos para: " + filePath);
                return;
            }

            // Remover el prefijo si existe (data:image/wsq;base64,)
            String base64Data = base64;
            if (base64.contains(",")) {
                base64Data = base64.split(",")[1];
                System.out.println("📁 Prefijo removido, datos restantes: " + base64Data.length() + " caracteres");
            }

            // Verificar que los datos base64 sean válidos
            if (!base64Data.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                System.out.println("⚠️ Advertencia: Datos base64 pueden no ser válidos para: " + filePath);
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            System.out.println("📊 Bytes decodificados: " + imageBytes.length + " bytes");

            Files.write(Paths.get(filePath), imageBytes);
            System.out.println("💾 Archivo guardado: " + filePath + " (" + imageBytes.length + " bytes)");

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error decodificando base64 para: " + filePath);
            System.err.println("   Error: " + e.getMessage());
            throw new IOException("Error decodificando imagen base64: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("❌ Error guardando imagen: " + filePath);
            System.err.println("   Error: " + e.getMessage());
            throw new IOException("Error guardando imagen: " + e.getMessage(), e);
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

    private void crearEstructuraCarpetas() {
        String[] carpetas = {
                CARPETA_BASE,
                CARPETA_BASE + File.separator + CARPETA_HUELLAS,
                CARPETA_BASE + File.separator + CARPETA_FOTOS,
                CARPETA_BASE + File.separator + CARPETA_FIRMAS,
                CARPETA_BASE + File.separator + CARPETA_JSON
        };

        for (String carpeta : carpetas) {
            File directorio = new File(carpeta);
            if (!directorio.exists()) {
                boolean creado = directorio.mkdirs();
                if (creado) {
                    System.out.println("📁 Carpeta creada: " + carpeta);
                } else {
                    System.err.println("❌ No se pudo crear carpeta: " + carpeta);
                }
            } else {
                System.out.println("📁 Carpeta ya existe: " + carpeta);
            }
        }
    }

    private void guardarJSONCompleto(String referencia, Map<Integer, HuellaResponse> huellas) throws IOException {
        // Crear objeto con todos los datos
        Map<String, Object> datos = new HashMap<>();
        datos.put("referencia", referencia);
        datos.put("fechaCaptura", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        datos.put("totalHuellas", huellas.size());

        // Lista de huellas por dedo
        List<Map<String, Object>> listaHuellas = new ArrayList<>();

        for (Map.Entry<Integer, HuellaResponse> entry : huellas.entrySet()) {
            int codigoDedo = entry.getKey();
            HuellaResponse huella = entry.getValue();

            Map<String, Object> datosHuella = new HashMap<>();
            datosHuella.put("codigoDedo", codigoDedo);
            datosHuella.put("nombreDedo", MapeadorDedos.obtenerNombreDedo(codigoDedo));
            datosHuella.put("mano", MapeadorDedos.obtenerMano(codigoDedo));
            datosHuella.put("exitoso", huella.isExitoso());
            datosHuella.put("mensaje", huella.getMensaje());
            datosHuella.put("calidad", huella.getCalidad());
            datosHuella.put("calidadImagen", huella.getCalidadImagen());
            datosHuella.put("calidadTotal", huella.getCalidadTotal());
            datosHuella.put("descripcionCalidad", huella.getDescripcionCalidad());
            datosHuella.put("minucias", huella.getMinucias());
            datosHuella.put("conteoMinucias", huella.getConteoMinucias());
            datosHuella.put("dispositivo", huella.getDispositivo());
            datosHuella.put("tieneImagenWsq", huella.tieneImagenWsqReal());
            datosHuella.put("tamanoImagenWsq", huella.getImagenWsq() != null ? huella.getImagenWsq().length() : 0);
            datosHuella.put("nombreArchivo", referencia + "_" + codigoDedo + ".wsq");

            listaHuellas.add(datosHuella);
        }

        datos.put("huellas", listaHuellas);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        String filename = CARPETA_BASE + File.separator + CARPETA_JSON + File.separator +
                referencia + ".json";

        mapper.writeValue(new File(filename), datos);
        System.out.println("📄 JSON guardado: " + filename);

        // También guardar versión TXT
        guardarTXTCompleto(referencia, huellas);
    }

    private void guardarTXTCompleto(String referencia, Map<Integer, HuellaResponse> huellas) throws IOException {
        String filename = CARPETA_BASE + File.separator + CARPETA_JSON + File.separator +
                referencia + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("REFERENCIA: " + referencia);
            writer.println("FECHA CAPTURA: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("TOTAL HUELLAS: " + huellas.size());
            writer.println();
            writer.println("=== DETALLE DE HUELLAS ===");

            for (Map.Entry<Integer, HuellaResponse> entry : huellas.entrySet()) {
                int codigoDedo = entry.getKey();
                HuellaResponse huella = entry.getValue();

                writer.println();
                writer.println("DEDO: " + MapeadorDedos.obtenerNombreDedo(codigoDedo) + " (Código: " + codigoDedo + ")");
                writer.println("Archivo: " + referencia + "_" + codigoDedo + ".wsq");
                writer.println("Calidad: " + huella.getCalidadTotal() + "% - " + huella.getDescripcionCalidad());
                writer.println("Minucias: " + huella.getMinuciasFormateadas());
                writer.println("Dispositivo: " + huella.getDispositivo());
                writer.println("Éxito: " + (huella.isExitoso() ? "SI" : "NO"));

                if (huella.getMensaje() != null && !huella.getMensaje().isEmpty()) {
                    writer.println("Mensaje: " + huella.getMensaje());
                }
            }
        }

        System.out.println("📝 TXT guardado: " + filename);
    }

    private void guardarImagenesPorDedo(String referencia, int codigoDedo, HuellaResponse huella) throws IOException {
        String codigoArchivo = MapeadorDedos.obtenerCodigoArchivo(codigoDedo);
        String nombreDedo = MapeadorDedos.obtenerNombreDedo(codigoDedo);

        // Guardar imagen WSQ si está disponible
        if (huella.tieneImagenWsqReal()) {
            String filename = CARPETA_BASE + File.separator + CARPETA_HUELLAS + File.separator +
                    referencia + "_" + codigoArchivo + ".wsq";

            guardarImagenBase64(huella.getImagenWsq(), filename);
            System.out.println("✅ Imagen WSQ guardada: " + referencia + "_" + codigoArchivo + ".wsq" +
                    " (" + nombreDedo + ")");
        } else {
            System.out.println("⚠️ No hay imagen WSQ real para " + nombreDedo);
        }

        // Guardar imagen BMP si está disponible
        if (huella.tieneImagenBmpReal()) {
            String filename = CARPETA_BASE + File.separator + CARPETA_HUELLAS + File.separator +
                    referencia + "_" + codigoArchivo + ".bmp";

            guardarImagenBase64(huella.getImagenBmp(), filename);
            System.out.println("✅ Imagen BMP guardada: " + referencia + "_" + codigoArchivo + ".bmp" +
                    " (" + nombreDedo + ")");
        }
    }
}