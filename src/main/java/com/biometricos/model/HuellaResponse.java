package com.biometricos.model;

public class HuellaResponse {
    private boolean exitoso;
    private String mensaje;
    private Integer calidad;
    private Integer minucias;
    private String dispositivo;
    private String imagenBase64;
    private String dedo;        // "4" para pulgar izquierdo, "5" para pulgar derecho
    private String mano;         // "izquierda" o "derecha"

    // Constructores, getters y setters
    public HuellaResponse() {}

    public HuellaResponse(boolean exitoso, String mensaje) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
    }

    // Getters y Setters
    public boolean isExitoso() {
        return exitoso;
    }

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

    public Integer getMinucias() {
        return minucias;
    }

    public void setMinucias(Integer minucias) {
        this.minucias = minucias;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

    public String getImagenBase64() {
        return imagenBase64;
    }

    public void setImagenBase64(String imagenBase64) {
        this.imagenBase64 = imagenBase64;
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

    @Override
    public String toString() {
        return "HuellaResponse{" +
                "exitoso=" + exitoso +
                ", mensaje='" + mensaje + '\'' +
                ", calidad=" + calidad +
                ", minucias=" + minucias +
                ", dispositivo='" + dispositivo + '\'' +
                ", dedo='" + dedo + '\'' +
                ", mano='" + mano + '\'' +
                '}';
    }
}
