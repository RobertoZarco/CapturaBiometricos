package com.biometricos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HuellaResponse {

    @JsonProperty("exito")
    private boolean exitoso;

    private String mensaje;

    @JsonProperty("calidad")
    private Integer calidad;

    @JsonProperty("minucias")
    private String minucias;

    @JsonProperty("dispositivo")
    private String dispositivo;

    // CORREGIDO: usar "imagenWsq" (minúscula) en lugar de "ImagenWsq"
    @JsonProperty("imagenWsq")
    private String imagenWsq;

    @JsonProperty("imagenBmp")
    private String imagenBmp;

    @JsonProperty("calidadImagen")
    private String calidadImagen;

    private String dedo;
    private String mano;

    // Getters y Setters
    public boolean isExitoso() {
        return exitoso;
    }

    @JsonProperty("exito")
    public void setExitoso(boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Integer getCalidad() {
        return calidad;
    }

    public void setCalidad(Integer calidad) {
        this.calidad = calidad;
    }

    public String getMinucias() {
        return minucias;
    }

    public void setMinucias(String minucias) {
        this.minucias = minucias;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

    public String getImagenWsq() {
        return imagenWsq;
    }

    @JsonProperty("imagenWsq")
    public void setImagenWsq(String imagenWsq) {
        this.imagenWsq = imagenWsq;
    }

    public String getImagenBmp() {
        return imagenBmp;
    }

    @JsonProperty("imagenBmp")
    public void setImagenBmp(String imagenBmp) {
        this.imagenBmp = imagenBmp;
    }

    public String getCalidadImagen() {
        return calidadImagen;
    }

    @JsonProperty("calidadImagen")
    public void setCalidadImagen(String calidadImagen) {
        this.calidadImagen = calidadImagen;
    }

    public String getDedo() {
        return dedo;
    }

    public void setDedo(String dedo) {
        this.dedo = dedo;
    }

    public String getMano() {
        return mano;
    }

    public void setMano(String mano) {
        this.mano = mano;
    }

    // Método para obtener la imagen (prioridad: WSQ -> BMP)
    public String getImagenBase64() {
        if (imagenWsq != null && !imagenWsq.isEmpty() && !imagenWsq.equals("BMP_262159_BYTES")) {
            return imagenWsq;
        } else if (imagenBmp != null && !imagenBmp.isEmpty() && !imagenBmp.equals("BMP_262159_BYTES")) {
            return imagenBmp;
        }
        return null;
    }

    // Método para verificar si tenemos imagen WSQ real
    public boolean tieneImagenWsqReal() {
        return imagenWsq != null &&
                !imagenWsq.isEmpty() &&
                !imagenWsq.equals("BMP_262159_BYTES") &&
                imagenWsq.length() > 100; // Debe ser un base64 significativo
    }

    // Método para verificar si tenemos imagen BMP real
    public boolean tieneImagenBmpReal() {
        return imagenBmp != null &&
                !imagenBmp.isEmpty() &&
                !imagenBmp.equals("BMP_262159_BYTES") &&
                imagenBmp.length() > 100;
    }

    // Método auxiliar para obtener el conteo de minucias
    public int getConteoMinucias() {
        if (minucias == null || minucias.trim().isEmpty()) {
            return 0;
        }
        String[] lineas = minucias.split("\n");
        return lineas.length;
    }

    // Método para formatear las minucias para mostrar
    public String getMinuciasFormateadas() {
        if (minucias == null || minucias.trim().isEmpty()) {
            return "0 minucias";
        }
        int conteo = getConteoMinucias();
        return conteo + " minucias";
    }

    // Método para obtener la calidad numérica
    public Integer getCalidadTotal() {
        if (calidad != null) {
            return calidad;
        }
        return mapearCalidadTextoANumero(calidadImagen);
    }

    private Integer mapearCalidadTextoANumero(String calidadTexto) {
        if (calidadTexto == null) return null;

        String textoLower = calidadTexto.toLowerCase().trim();
        switch (textoLower) {
            case "excelente": return 90;
            case "muy buena": return 80;
            case "buena": return 70;
            case "regular": return 60;
            case "mala": return 40;
            case "muy mala": return 20;
            default: return null;
        }
    }

    public String getDescripcionCalidad() {
        if (calidadImagen != null && !calidadImagen.matches("\\d+")) {
            return calidadImagen;
        }
        Integer calidadNum = getCalidadTotal();
        if (calidadNum == null) return "Desconocida";
        if (calidadNum >= 90) return "Excelente";
        if (calidadNum >= 80) return "Muy Buena";
        if (calidadNum >= 70) return "Buena";
        if (calidadNum >= 60) return "Regular";
        if (calidadNum >= 40) return "Mala";
        return "Muy Mala";
    }

    // Método para logging de imágenes
    public void logInfoImagenes() {
        System.out.println("📷 INFORMACIÓN DE IMÁGENES:");
        System.out.println("   imagenWsq: " + (tieneImagenWsqReal() ?
                imagenWsq.length() + " caracteres (REAL)" : "no disponible"));
        System.out.println("   imagenBmp: " + (tieneImagenBmpReal() ?
                imagenBmp.length() + " caracteres (REAL)" : "placeholder o vacío"));
        System.out.println("   imagenBase64(): " + (getImagenBase64() != null ?
                getImagenBase64().length() + " caracteres" : "null"));
    }
}